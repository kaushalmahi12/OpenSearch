/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
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
    public static class SandboxStatsHolder implements Writeable, ToXContentFragment {
        private final  long completedTasks;
        private final long cancelledTasks;
        private final long runningTasks;
        private final Map<String, Double> resourceUsage;

        public SandboxStatsHolder(long completedTasks, long cancelledTasks, long runningTasks, Map<String, Double> resourceUsage) {
            this.completedTasks = completedTasks;
            this.cancelledTasks = cancelledTasks;
            this.runningTasks = runningTasks;
            this.resourceUsage = resourceUsage;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVLong(completedTasks);
            out.writeVLong(cancelledTasks);
            out.writeVLong(runningTasks);
            out.writeMap(resourceUsage, StreamOutput::writeString, StreamOutput::writeDouble);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field("completed_tasks", completedTasks);
            builder.field("cancelled_tasks", cancelledTasks);
            builder.field("running_tasks", runningTasks);
            builder.startObject("resource_usage");
            for (Map.Entry<String, Double> resource: resourceUsage.entrySet()) {
                builder.field(resource.getKey(), resource.getValue());
            }
            builder.endObject();
            return builder;
        }
    }

}
