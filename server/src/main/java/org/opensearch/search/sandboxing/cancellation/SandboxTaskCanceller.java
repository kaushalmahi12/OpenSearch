/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.cancellation;

/**
 * This class is used to identify and cancel the violating tasks in a resourceLimitGroup
 */
public abstract class SandboxTaskCanceller {
    protected final TaskCancellationStrategy cancellationStrategy;

    public SandboxTaskCanceller(TaskCancellationStrategy cancellationStrategy) {
        this.cancellationStrategy = cancellationStrategy;
    }

    public abstract void cancelTasks(CancellationContext cancellationContext);
}
