/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.module;

import org.opensearch.common.inject.AbstractModule;
import org.opensearch.search.sandboxing.SandboxPruner;
import org.opensearch.search.sandboxing.cancellation.SandboxTaskCanceller;
import org.opensearch.search.sandboxing.tracker.SandboxResourceUsageTracker;
import org.opensearch.search.sandboxing.tracker.SandboxResourceUsageTrackerService;

/**
 * Module class for resource usage limiting  related artifacts
 */
public class SandboxModule extends AbstractModule {

    /**
     * Default constructor
     */
    public SandboxModule() {}

    @Override
    protected void configure() {
        bind(SandboxResourceUsageTracker.class).to(SandboxResourceUsageTrackerService.class).asEagerSingleton();
        bind(SandboxTaskCanceller.class).to(SandboxResourceUsageTrackerService.class).asEagerSingleton();
        bind(SandboxPruner.class).to(SandboxResourceUsageTrackerService.class).asEagerSingleton();
    }
}
