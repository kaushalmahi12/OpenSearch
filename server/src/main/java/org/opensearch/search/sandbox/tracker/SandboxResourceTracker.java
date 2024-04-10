/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox.tracker;

/**
 * This interface is mainly for tracking the sandbox level resource usages
 */
public interface SandboxResourceTracker {
    /**
     * updates the current resource usage of sandboxes
     */
    public void updateSandboxResourceUsages();
}
