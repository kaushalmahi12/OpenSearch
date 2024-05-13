/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing;

import org.opensearch.tasks.Task;

public enum SandboxResourceType {
  JVM {
    @Override
    public long getResourceUsage(Task task) {
      return task.getTotalResourceStats().getMemoryInBytes();
    }
  },
  CPU {
    @Override
    public long getResourceUsage(Task task) {
      return task.getTotalResourceStats().getCpuTimeInNanos();
    }
  };

  public abstract long getResourceUsage(Task task);

  public static SandboxResourceType fromString(String type) {
    for (SandboxResourceType resourceType : values()) {
      if (resourceType.name().equalsIgnoreCase(type)) {
        return resourceType;
      }
    }
    throw new IllegalArgumentException("Unsupported resource type: " + type);
  }
}
