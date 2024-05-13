/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.cancellation;

import org.opensearch.cluster.metadata.Sandbox;
import org.opensearch.search.sandboxing.SandboxResourceTaskComposite;

import java.util.List;

public class CancellationContext {
  private final SandboxResourceTaskComposite sandboxResourceTaskComposite;
  private final List<Sandbox> activeSandboxes;

  public CancellationContext(
      SandboxResourceTaskComposite sandboxResourceTaskComposite,
      List<Sandbox> activeSandboxes
  ) {
    this.sandboxResourceTaskComposite = sandboxResourceTaskComposite;
    this.activeSandboxes = activeSandboxes;
  }

  public SandboxResourceTaskComposite getSandboxResourceTaskComposite() {
    return sandboxResourceTaskComposite;
  }

  public List<Sandbox> getActiveSandboxes() {
    return activeSandboxes;
  }
}
