/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.cancellation;

import org.opensearch.cluster.metadata.Sandbox;
import org.opensearch.search.sandboxing.SandboxResourceType;
import org.opensearch.tasks.CancellableTask;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskCancellation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensearch.search.sandboxing.tracker.SandboxResourceUsageTrackerService.TRACKED_RESOURCES;

public class DefaultTaskCanceller extends SandboxTaskCanceller {
  private CancellationContext cancellationContext;

  public DefaultTaskCanceller(TaskCancellationStrategy cancellationStrategy) {
    super(cancellationStrategy);
  }

  @Override
  public void cancelTasks(CancellationContext cancellationContext) {
    this.cancellationContext = cancellationContext;
    List<TaskCancellation> cancellableTasks = getAllCancellableTasks();
    for (TaskCancellation taskCancellation : cancellableTasks) {
      taskCancellation.cancel();
    }
  }

  /**
   * @return list of cancellable tasks
   */
  private List<TaskCancellation> getAllCancellableTasks() {
    final List<Sandbox> inViolationSandboxes = getSandboxesInViolation();
    final List<Sandbox> enforcedSandboxes = inViolationSandboxes.stream()
        .filter(sandbox -> sandbox.getMode().equals(Sandbox.SandboxMode.ENFORCED))
        .collect(Collectors.toList());

    List<TaskCancellation> cancellableTasks = new ArrayList<>();

    for (Sandbox sandbox : enforcedSandboxes) {
      cancellableTasks.addAll(getCancellableTasksFrom(sandbox));
    }
    return cancellableTasks;
  }

  List<TaskCancellation> getCancellableTasksFrom(Sandbox sandbox) {
    List<TaskCancellation> cancellations = new ArrayList<>();
    for (SandboxResourceType resourceType : TRACKED_RESOURCES) {
      final long reduceBy = getUsage(sandbox, resourceType) - sandbox.getResourceLimitFor(resourceType).getThresholdInLong();
      // TODO if the resource is not defined for this sandbox then ignore cancellations from it
      if (reduceBy < 0.0) {
        continue;
      }
      List<TaskCancellation> taskCancellations = cancellationStrategy.selectTasksToCancel(
              getTasksBySandbox(sandbox.getName()),
              reduceBy,
              resourceType)
          .stream()
          .map(task -> createTaskCancellation((CancellableTask) task))
          .collect(Collectors.toList());

      cancellations.addAll(taskCancellations);
    }
    return cancellations;
  }

  private List<Sandbox> getSandboxesInViolation() {
    final List<Sandbox> breachingSandboxNames = new ArrayList<>();

    for (Sandbox sandbox : getActiveSandboxes()) {
      Map<SandboxResourceType, Long> currentResourceUsage = getResourceUsage().get(sandbox.getName());
      boolean isBreaching = false;

      for (Sandbox.ResourceLimit resourceLimit : sandbox.getResourceLimits()) {
        if (currentResourceUsage.get(resourceLimit.getResourceType()) > resourceLimit.getThreshold()) {
          isBreaching = true;
          break;
        }
      }
      if (isBreaching) breachingSandboxNames.add(sandbox);
    }

    return breachingSandboxNames;
  }

  private Long getUsage(Sandbox sandbox, SandboxResourceType resourceType) {
    return getResourceUsage().get(sandbox.getName()).get(resourceType);
  }

  private TaskCancellation createTaskCancellation(CancellableTask task) {
    // todo add reasons and callbacks
    return new TaskCancellation(task, List.of(), List.of(this::callbackOnCancel));
  }

  private void callbackOnCancel() {
    // Implement callback logic here
    System.out.println("Task was cancelled.");
  }

  private Map<String, Map<SandboxResourceType, Long>> getResourceUsage() {
    return cancellationContext.getSandboxResourceTaskComposite().getResourceUsageData().getResourceUsage();
  }

  private Map<String, List<Task>> getTasksBySandboxes() {
    return cancellationContext.getSandboxResourceTaskComposite().getTaskData().getTasksBySandbox();
  }

  private List<Task> getTasksBySandbox(String sandboxName) {
    return getTasksBySandboxes().get(sandboxName);
  }

  private List<Sandbox> getActiveSandboxes() {
    return cancellationContext.getActiveSandboxes();
  }
}
