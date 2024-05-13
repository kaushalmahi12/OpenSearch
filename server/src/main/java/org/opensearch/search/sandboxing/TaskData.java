/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing;

import java.util.List;
import java.util.Map;

import org.opensearch.tasks.Task;

public class TaskData {
  private final Map<String, List<Task>> tasksBySandboxes;

  public TaskData(Map<String, List<Task>> tasksBySandboxes) {
    this.tasksBySandboxes = tasksBySandboxes;
  }

  public Map<String, List<Task>> getTasksBySandbox() {
    return tasksBySandboxes;
  }
}
