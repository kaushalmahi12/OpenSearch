/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import java.util.concurrent.atomic.AtomicLong;

public class SandboxStats {
    private AtomicLong completedTasks = new AtomicLong(0);
    private AtomicLong cancelledTasks = new AtomicLong(0);
    private AtomicLong runningTasks = new AtomicLong(0);

    public void incrementCompletedTasks() {
        completedTasks.incrementAndGet();
    }

    public void incrementCancelledTasks() {
        cancelledTasks.incrementAndGet();
    }

    public void incrementRunningTasks() {
        runningTasks.incrementAndGet();
    }

    public void decrementRunningTasks() {
        runningTasks.decrementAndGet();
    }

}
