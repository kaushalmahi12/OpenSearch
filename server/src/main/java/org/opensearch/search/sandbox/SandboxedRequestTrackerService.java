/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskCancellation;
import org.opensearch.tasks.TaskManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class tracks requests per sandboxes
 */
public class SandboxedRequestTrackerService implements TaskManager.TaskEventListeners {
    /**
     * Sandbox ids which are marked for deletion in between the @link SandboxService runs
     */

    private ConcurrentHashMap<String, Set<Task>> tasksPerSandbox;
    private Map<String, Sandbox> availableSandboxes;
    private List<String> toDeleteSandboxes;
    /**
     * It is used to track the task to sandbox mapping which will be useful to remove it from the @link tasksPerSandbox
     */
    private ConcurrentHashMap<Long, String> taskToSandboxMapping;
    private ConcurrentHashMap<String, SandboxStats> sandboxStats;
    private TaskManager taskManager;


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
        //TODO: add implementaion later
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
        String sandboxId = taskToSandboxMapping.get(task.getId());
        if (sandboxId == null) {
            // Sandbox could be deleted
            if (toDeleteSandboxes.contains(sandboxId)) {
                return;
            } else {
                throw  new IllegalStateException("Sandbox should have been present for this ");
            }
        }
        final SandboxStats sandboxStats1 = sandboxStats.get(sandboxId);
        sandboxStats1.incrementCompletedTasks();
        sandboxStats1.decrementRunningTasks();
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

    private List<String> getBreachingSandboxIds() {
        return null;
    }

    private List<TaskCancellation> getCancellableTasksFrom(String sandboxId) {
        //TODO: TaskCancellation should have the callback to increment cancellation for this sandbox id
        return null;
    }

    public List<Sandbox> getAvailableSandboxes() {
        return availableSandboxes.values()
                .stream().collect(Collectors.toUnmodifiableList());
    }
}
