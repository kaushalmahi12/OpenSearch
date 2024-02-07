/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.core.tasks.resourcetracker.TaskResourceUsage;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskCancellation;
import org.opensearch.tasks.TaskManager;
import org.opensearch.tasks.TaskResourceTrackingService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * This class tracks requests per sandboxes
 */
public class SandboxedRequestTrackerService implements TaskManager.TaskEventListeners {

    private static final String CPU = "CPU";
    private static final String JVM_ALLOCATIONS = "JVM_Allocations";
    private static final int numberOfAvailableProcessors = Runtime.getRuntime().availableProcessors();
    private static final long totalAvailableJvmMemory = Runtime.getRuntime().totalMemory();
    private final LongSupplier timeNanosSupplier;
    /**
     * Sandbox ids which are marked for deletion in between the @link SandboxService runs
     */

    private final Map<String, Set<Task>> tasksPerSandbox;
    private final Map<String, Sandbox> availableSandboxes;
    private List<String> toDeleteSandboxes;
    /**
     * It is used to track the task to sandbox mapping which will be useful to remove it from the @link tasksPerSandbox
     */
    private final Map<Long, String> taskToSandboxMapping;
    private final Map<String, SandboxStats> sandboxStats;
    private final TaskManager taskManager;
    private final TaskResourceTrackingService taskResourceTrackingService;

    public SandboxedRequestTrackerService(TaskManager taskManager, TaskResourceTrackingService taskResourceTrackingService,
                                          LongSupplier timeNanosSupplier) {
        this.taskManager = taskManager;
        this.taskResourceTrackingService = taskResourceTrackingService;
        availableSandboxes = new ConcurrentHashMap<>();
        tasksPerSandbox = new ConcurrentHashMap<>();
        toDeleteSandboxes = Collections.synchronizedList(new ArrayList<>());
        taskToSandboxMapping = new ConcurrentHashMap<>();
        sandboxStats = new ConcurrentHashMap<>();
        this.timeNanosSupplier = timeNanosSupplier;
    }

    public boolean startTracking(Task task, Sandbox sandbox) {
        taskToSandboxMapping.put(task.getId(), sandbox.get_id());
        tasksPerSandbox.compute(sandbox.get_id(), (sandboxId, tasks) -> {
            if (tasks == null) {
                tasks = ConcurrentHashMap.newKeySet();
            }
            tasks.add(task);
            return tasks;
        });
        sandboxStats.compute(sandbox.get_id(), (k, v) -> {
            if (v == null) {
                v = new SandboxStats();
            }
            v.incrementRunningTasks();
            return v;
        });
        return true;
    }

    public void updateSandboxResourceUsages() {
        Map<String, Set<Task>> currentTasksPerSandbox = Collections.unmodifiableMap(tasksPerSandbox);
        for (Map.Entry<String, Set<Task>> sandboxView: currentTasksPerSandbox.entrySet()) {
            taskResourceTrackingService.refreshResourceStats(sandboxView.getValue().toArray(new Task[0]));
            long cpuTimeInNanos = 0;
            long jvmAllocations = 0;
            AbsoluteResourceUsage sandboxLevelAbsoluteResourceUsage = new AbsoluteResourceUsage(0, 0);
            for (Task task: sandboxView.getValue()) {
                AbsoluteResourceUsage taskLevelAbsoluteResourceUsage = calculateAbsoluteResourceUsageFor(task);
                sandboxLevelAbsoluteResourceUsage = AbsoluteResourceUsage.merge(sandboxLevelAbsoluteResourceUsage, taskLevelAbsoluteResourceUsage);
            }
            // convert the usage into percentage
            sandboxStats
                .get(sandboxView.getKey())
                .setResourceUsage(Map.of(
                    CPU,
                    sandboxLevelAbsoluteResourceUsage.getAbsoluteCpuUsageInPercentage(),
                    JVM_ALLOCATIONS,
                    sandboxLevelAbsoluteResourceUsage.getAbsoluteJvmAllocationsUsageInPercent()
                ));
        }
    }

    private AbsoluteResourceUsage calculateAbsoluteResourceUsageFor(Task task) {
        TaskResourceUsage taskResourceUsage = task.getTotalResourceStats();
        long cpuTimeInNanos = taskResourceUsage.getCpuTimeInNanos();
        long jvmAllocations = taskResourceUsage.getMemoryInBytes();
        long taskElapsedTime = timeNanosSupplier.getAsLong() - task.getStartTimeNanos();
        return new AbsoluteResourceUsage(
            (cpuTimeInNanos * 1.0f) / (taskElapsedTime * numberOfAvailableProcessors) ,
            ((jvmAllocations * 1.0f) / totalAvailableJvmMemory)
        );
    }

    private static class AbsoluteResourceUsage {
        private final double absoluteCpuUsage;
        private final double absoluteJvmAllocationsUsage;

        public AbsoluteResourceUsage(double absoluteCpuUsage, double absoluteJvmAllocationsUsage) {
            this.absoluteCpuUsage = absoluteCpuUsage;
            this.absoluteJvmAllocationsUsage = absoluteJvmAllocationsUsage;
        }

        public static AbsoluteResourceUsage merge(AbsoluteResourceUsage a, AbsoluteResourceUsage b) {
            return new AbsoluteResourceUsage(a.absoluteCpuUsage + b.absoluteCpuUsage,
                a.absoluteJvmAllocationsUsage + b.absoluteJvmAllocationsUsage);
        }

        public double getAbsoluteCpuUsageInPercentage() {
            return absoluteCpuUsage * 100;
        }

        public double getAbsoluteJvmAllocationsUsageInPercent() {
            return absoluteJvmAllocationsUsage * 100;
        }
    }

    public void pruneSandboxes() {
        toDeleteSandboxes = toDeleteSandboxes
            .stream()
            .filter(this::hasUnfinishedTasks)
            .collect(Collectors.toList());
    }

    public boolean addSandbox(Sandbox sandbox) {
        availableSandboxes.put(sandbox.get_id(), sandbox);
        return true;
    }

    public boolean updateSandbox(Sandbox sandbox) {
        return addSandbox(sandbox);
    }


    public boolean removeSandbox(Sandbox sandbox) {
        availableSandboxes.remove(sandbox.get_id());
        if (hasUnfinishedTasks(sandbox.get_id())) {
            toDeleteSandboxes.add(sandbox.get_id());
        }
        return true;
    }

    public boolean hasUnfinishedTasks(String sandboxId) {
        return tasksPerSandbox.get(sandboxId) != null && !tasksPerSandbox.get(sandboxId).isEmpty();
    }

    @Override
    public void onTaskCompleted(Task task) {
        String completedTaskSandboxId = taskToSandboxMapping.get(task.getId());
        if (toDeleteSandboxes.contains(completedTaskSandboxId)) {
            return;
        }
        final SandboxStats completedTaskSandboxStats = sandboxStats.get(completedTaskSandboxId);
        completedTaskSandboxStats.incrementCompletedTasks();
        completedTaskSandboxStats.decrementRunningTasks();
    }

    public void cancelViolatingTasks() {
        List<TaskCancellation> cancellableTasks = getCancellableTasks();
        for (TaskCancellation taskCancellation: cancellableTasks) {
            taskCancellation.cancel();
        }
    }

    private List<TaskCancellation> getCancellableTasks() {
        List<String> inViolationSandboxes = getBreachingSandboxIds();
        List<TaskCancellation> cancellableTasks = new ArrayList<>();
        for (String sandboxId: inViolationSandboxes) {
            cancellableTasks.addAll(getCancellableTasksFrom(sandboxId));
        }
        return cancellableTasks;
    }

    public Map<String, SandboxStats.SandboxStatsHolder> getSandboxLevelStats() {
        Map<String, SandboxStats.SandboxStatsHolder> sandboxLevelStats = new HashMap<>();
        for (String sandboxId: sandboxStats.keySet()) {
            sandboxLevelStats.put(sandboxId, sandboxStats.get(sandboxId).getStats());
        }
        return sandboxLevelStats;
    }

    private List<String> getBreachingSandboxIds() {
        return Collections.emptyList();
    }

    private List<TaskCancellation> getCancellableTasksFrom(String sandboxId) {
        //TODO: TaskCancellation should have the callback to increment cancellation for this sandbox id
        return Collections.emptyList();
    }

    public List<Sandbox> getAvailableSandboxes() {
        return availableSandboxes.values()
                .stream().collect(Collectors.toUnmodifiableList());
    }
}
