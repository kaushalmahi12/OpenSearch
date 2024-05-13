/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing;

public class SandboxResourceTaskComposite {
  ResourceUsageData resourceUsageData;
  TaskData taskData;

  public SandboxResourceTaskComposite(ResourceUsageData resourceUsageData, TaskData taskData) {
    this.resourceUsageData = resourceUsageData;
    this.taskData = taskData;
  }

  public ResourceUsageData getResourceUsageData() {
    return resourceUsageData;
  }

  public TaskData getTaskData() {
    return taskData;
  }
}
