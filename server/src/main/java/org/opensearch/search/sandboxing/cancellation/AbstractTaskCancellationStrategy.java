/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.cancellation;

import org.opensearch.search.sandboxing.SandboxResourceType;
import org.opensearch.tasks.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTaskCancellationStrategy implements TaskCancellationStrategy {

  public abstract Comparator<Task> sortingCondition();

  @Override
  public List<Task> selectTasksToCancel(List<Task> tasks, long reduceBy, SandboxResourceType resourceType) {
    List<Task> sortedTasks = tasks.stream()
        .sorted(sortingCondition())
        .collect(Collectors.toList());

    List<Task> selectedTasks = new ArrayList<>();
    long accumulated = 0;

    for (Task task : sortedTasks) {
      selectedTasks.add(task);
      accumulated += resourceType.getResourceUsage(task);
      if (accumulated >= reduceBy) {
        break;
      }
    }
    return selectedTasks;
  }
}
