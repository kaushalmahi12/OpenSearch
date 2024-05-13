/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing;

import java.util.Map;

public class ResourceUsageData {
  private final Map<String, Map<SandboxResourceType, Long>> resourceUsage;

  public ResourceUsageData(Map<String, Map<SandboxResourceType, Long>> resourceUsage) {
    this.resourceUsage = resourceUsage;
  }

  public Map<String, Map<SandboxResourceType, Long>> getResourceUsage() {
    return resourceUsage;
  }
}
