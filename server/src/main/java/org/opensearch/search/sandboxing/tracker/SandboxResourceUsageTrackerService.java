/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.tracker;

import org.opensearch.common.inject.Inject;
import org.opensearch.core.tasks.resourcetracker.TaskResourceUsage;
import org.opensearch.search.sandboxing.SandboxResourceTaskComposite;
import org.opensearch.search.sandboxing.SandboxResourceType;
import org.opensearch.search.sandboxing.SandboxTask;
import org.opensearch.search.sandboxing.ResourceUsageData;
import org.opensearch.search.sandboxing.TaskData;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;
import org.opensearch.tasks.TaskResourceTrackingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * This class tracks requests per Sandbox
 */
public class SandboxResourceUsageTrackerService implements
    SandboxResourceUsageTracker,
        TaskManager.TaskEventListeners {

    public static final List<SandboxResourceType> TRACKED_RESOURCES = List.of(SandboxResourceType.JVM);

    private final LongSupplier timeNanosSupplier;
    private final TaskManager taskManager;
    private final TaskResourceTrackingService taskResourceTrackingService;

    /**
     * SandboxResourceTrackerService constructor
     * @param taskManager
     * @param taskResourceTrackingService
     * @param timeNanosSupplier
     */
    @Inject
    public SandboxResourceUsageTrackerService(
        final TaskManager taskManager,
        final TaskResourceTrackingService taskResourceTrackingService,
        final LongSupplier timeNanosSupplier
    ) {
        this.taskManager = taskManager;
        this.taskResourceTrackingService = taskResourceTrackingService;
        this.timeNanosSupplier = timeNanosSupplier;
    }

    @Override
    public SandboxResourceTaskComposite getSandboxResourceTaskComposite() {
        Map<String, List<Task>> tasksBySandbox = getTasksGroupedBySandbox();
        Map<String, Map<SandboxResourceType, Long>> sandboxResourceUsage = getResourceUsageOfSandboxes(tasksBySandbox);

        TaskData taskData = new TaskData(tasksBySandbox);
        ResourceUsageData resourceUsageData = new ResourceUsageData(sandboxResourceUsage);
        return new SandboxResourceTaskComposite(resourceUsageData, taskData);
    }

    private Map<String, List<Task>> getTasksGroupedBySandbox() {
        return taskResourceTrackingService.getResourceAwareTasks()
            .values()
            .stream()
            .filter(SandboxTask.class::isInstance)
            .map(SandboxTask.class::cast)
            .collect(Collectors.groupingBy(
                SandboxTask::getSandboxName,
                Collectors.mapping(task -> (Task) task, Collectors.toList())
            ));
    }

    private Map<String, Map<SandboxResourceType, Long>> getResourceUsageOfSandboxes(Map<String, List<Task>> tasksBySandbox) {
        Map<String, Map<SandboxResourceType, Long>> resourceUsage = new HashMap<>();

        // Iterate over each sandbox entry
        for (Map.Entry<String, List<Task>> sandboxEntry : tasksBySandbox.entrySet()) {
            String sandboxName = sandboxEntry.getKey();
            List<Task> tasks = sandboxEntry.getValue();

            // Prepare a usage map for the current sandbox, or retrieve the existing one
            Map<SandboxResourceType, Long> sandboxUsage = resourceUsage.computeIfAbsent(sandboxName, k -> new HashMap<>());

            // Accumulate resource usage for each task in the sandbox
            for (Task task : tasks) {
                for (SandboxResourceType resourceType: TRACKED_RESOURCES) {
                    sandboxUsage.put(resourceType, resourceType.getResourceUsage(task));
                    resourceUsage.put(sandboxName, sandboxUsage);
                }
            }
        }
        return resourceUsage;
    }

    /**
     * method to handle the completed tasks
     * @param task represents completed task on the node
     */
    @Override
    public void onTaskCompleted(Task task) {}
}
