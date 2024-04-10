/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox.tracker;

import org.opensearch.common.inject.Inject;
import org.opensearch.core.tasks.resourcetracker.TaskResourceUsage;
import org.opensearch.search.sandbox.SandboxPruner;
import org.opensearch.search.sandbox.cancellation.SandboxRequestCanceller;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskCancellation;
import org.opensearch.tasks.TaskManager;
import org.opensearch.tasks.TaskResourceTrackingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * This class tracks requests per sandboxes
 */
public class SandboxResourceTrackerService
    implements
        TaskManager.TaskEventListeners,
        SandboxResourceTracker,
        SandboxRequestCanceller,
    SandboxPruner {

    private static final String CPU = "CPU";
    private static final String JVM_ALLOCATIONS = "JVM_Allocations";
    private static final int numberOfAvailableProcessors = Runtime.getRuntime().availableProcessors();
    private static final long totalAvailableJvmMemory = Runtime.getRuntime().totalMemory();
    private final LongSupplier timeNanosSupplier;
    /**
     * Sandbox ids which are marked for deletion in between the @link SandboxService runs
     */
    private List<String> toDeleteSandboxes;
    private List<Object> activeSandboxes;
    private final TaskManager taskManager;
    private final TaskResourceTrackingService taskResourceTrackingService;

    /**
     * SandboxResourceTrackerService constructor
     * @param taskManager
     * @param taskResourceTrackingService
     */
    @Inject
    public SandboxResourceTrackerService(
        TaskManager taskManager,
        TaskResourceTrackingService taskResourceTrackingService
    ) {
        this.taskManager = taskManager;
        this.taskResourceTrackingService = taskResourceTrackingService;
        toDeleteSandboxes = Collections.synchronizedList(new ArrayList<>());
        this.timeNanosSupplier = System::nanoTime;
    }

    @Override
    public void updateSandboxResourceUsages() {

    }

    // @Override
    // public SandboxStatsHolder getStats() {
    // return null;
    // }

    private AbsoluteResourceUsage calculateAbsoluteResourceUsageFor(Task task) {
        TaskResourceUsage taskResourceUsage = task.getTotalResourceStats();
        long cpuTimeInNanos = taskResourceUsage.getCpuTimeInNanos();
        long jvmAllocations = taskResourceUsage.getMemoryInBytes();
        long taskElapsedTime = timeNanosSupplier.getAsLong() - task.getStartTimeNanos();
        return new AbsoluteResourceUsage(
            (cpuTimeInNanos * 1.0f) / (taskElapsedTime * numberOfAvailableProcessors),
            ((jvmAllocations * 1.0f) / totalAvailableJvmMemory)
        );
    }

    /**
     * Value holder class for resource usage in absolute terms with respect to system/process mem
     */
    private static class AbsoluteResourceUsage {
        private final double absoluteCpuUsage;
        private final double absoluteJvmAllocationsUsage;

        public AbsoluteResourceUsage(double absoluteCpuUsage, double absoluteJvmAllocationsUsage) {
            this.absoluteCpuUsage = absoluteCpuUsage;
            this.absoluteJvmAllocationsUsage = absoluteJvmAllocationsUsage;
        }

        public static AbsoluteResourceUsage merge(AbsoluteResourceUsage a, AbsoluteResourceUsage b) {
            return new AbsoluteResourceUsage(
                a.absoluteCpuUsage + b.absoluteCpuUsage,
                a.absoluteJvmAllocationsUsage + b.absoluteJvmAllocationsUsage
            );
        }

        public double getAbsoluteCpuUsageInPercentage() {
            return absoluteCpuUsage * 100;
        }

        public double getAbsoluteJvmAllocationsUsageInPercent() {
            return absoluteJvmAllocationsUsage * 100;
        }
    }

    /**
     * filter out the deleted sandboxes which still has unfi
     */
    public void pruneSandboxes() {
        toDeleteSandboxes = toDeleteSandboxes.stream().filter(this::hasUnfinishedTasks).collect(Collectors.toList());
    }

    private boolean hasUnfinishedTasks(String sandboxId) {
        return false;
    }

    /**
     * method to handle the completed tasks
     * @param task represents completed task on the node
     */
    @Override
    public void onTaskCompleted(Task task) {}

    /**
     * This method will select the sandboxes violating the enforced constraints
     * and cancel the tasks from the violating sandboxes
     * Cancellation happens in two scenarios
     * <ol>
     *     <li> If the sandbox is of enforced type and it is breaching its cancellation limit for the threshold </li>
     *     <li> Node is in duress and sandboxes which are breaching the cancellation thresholds will have cancellations </li>
     * </ol>
     */
    @Override
    public void cancelViolatingTasks() {
        List<TaskCancellation> cancellableTasks = getCancellableTasks();
        for (TaskCancellation taskCancellation : cancellableTasks) {
            taskCancellation.cancel();
        }
    }

    private List<TaskCancellation> getCancellableTasks() {
        // perform cancellations from enforced type sandboxes
        List<String> inViolationSandboxes = getBreachingSandboxIds();
        List<TaskCancellation> cancellableTasks = new ArrayList<>();
        for (String sandboxId : inViolationSandboxes) {
            cancellableTasks.addAll(getCancellableTasksFrom(sandboxId));
        }

        // perform cancellations from soft type sandboxes if the node is in duress (hitting node level cancellation
        // threshold)


        return cancellableTasks;
    }

    public void deleteSandbox(String sandboxId) {
        if (hasUnfinishedTasks(sandboxId)) {
            toDeleteSandboxes.add(sandboxId);
        }
        // remove this sandbox from the active sandboxes
    }

    private List<String> getBreachingSandboxIds() {
        return Collections.emptyList();
    }

    private List<TaskCancellation> getCancellableTasksFrom(String sandboxId) {
        return Collections.emptyList();
    }
}
