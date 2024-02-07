/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main class keeps track of live view of sandbox level stats except resourceUsage which is from last run of @link SandboxService
 * The inner class @link SandboxStats.SandboxStatsHolder holds a point in time view of sandbox stats
 */
public class SandboxStats {
    private final AtomicLong completedTasks = new AtomicLong(0);
    private final AtomicLong cancelledTasks = new AtomicLong(0);
    private final AtomicLong runningTasks = new AtomicLong(0);

    private Map<String, Double> resourceUsage;

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

    public void setResourceUsage(Map<String, Double> resourceUsage) {
        this.resourceUsage = resourceUsage;
    }

    public SandboxStatsHolder getStats() {
        return new SandboxStatsHolder(completedTasks.get(),
            cancelledTasks.get(),
            completedTasks.get(),
            Collections.unmodifiableMap(resourceUsage)
            );
    }

    /**
     * This class holds sandbox level stats
     */
    public static class SandboxStatsHolder {
        private long completedTasks;
        private long cancelledTasks;
        private long runningTasks;
        private Map<String, Double> resourceUsage;

        public SandboxStatsHolder(long completedTasks, long cancelledTasks, long runningTasks, Map<String, Double> resourceUsage) {
            this.completedTasks = completedTasks;
            this.cancelledTasks = cancelledTasks;
            this.runningTasks = runningTasks;
            this.resourceUsage = resourceUsage;
        }
    }

}
