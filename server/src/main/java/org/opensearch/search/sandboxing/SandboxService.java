/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.metadata.Sandbox;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.lifecycle.AbstractLifecycleComponent;
import org.opensearch.search.sandboxing.cancellation.CancellationContext;
import org.opensearch.search.sandboxing.cancellation.SandboxTaskCanceller;
import org.opensearch.search.sandboxing.tracker.SandboxResourceUsageTracker;
import org.opensearch.threadpool.Scheduler;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main service which will run periodically to track and cancel resource constraint violating tasks in resourceLimitGroups
 */
public class SandboxService extends AbstractLifecycleComponent {
    private static final Logger logger = LogManager.getLogger(SandboxService.class);

    private final SandboxResourceUsageTracker requestTracker;
    private final SandboxPruner resourceLimitGroupPruner;
    private volatile Scheduler.Cancellable scheduledFuture;
    private final SandboxServiceSettings sandboxServiceSettings;
    private final ThreadPool threadPool;
    private final SandboxTaskCanceller taskCanceller;
    private final ClusterService clusterService;

    /**
     * Guice managed constructor
     * @param requestTrackerService
     * @param sandboxPruner
     * @param sandboxServiceSettings
     * @param threadPool
     */
    @Inject
    public SandboxService(
        SandboxResourceUsageTracker requestTrackerService,
        SandboxServiceSettings sandboxServiceSettings,
        SandboxPruner sandboxPruner,
        SandboxTaskCanceller taskCanceller,
        ClusterService clusterService,
        ThreadPool threadPool
    ) {
        this.requestTracker = requestTrackerService;
        this.sandboxServiceSettings = sandboxServiceSettings;
        this.resourceLimitGroupPruner = sandboxPruner;
        this.taskCanceller = taskCanceller;
        this.clusterService = clusterService;
        this.threadPool = threadPool;
    }

    /**
     * run at regular interval
     */
    private void doRun() {
        SandboxResourceTaskComposite sandboxResourceTaskComposite = requestTracker.getSandboxResourceTaskComposite();

        CancellationContext cancellationContext = new CancellationContext(
            sandboxResourceTaskComposite,
            getActiveSandboxes()
        );
        taskCanceller.cancelTasks(cancellationContext);
        resourceLimitGroupPruner.pruneSandboxes();
    }

    private List<Sandbox> getActiveSandboxes() {
        return new ArrayList<>(clusterService.state().metadata().resourceLimitGroups().values());
    }

    /**
     * {@link AbstractLifecycleComponent} lifecycle method
     */
    @Override
    protected void doStart() {
        scheduledFuture = threadPool.scheduleWithFixedDelay(() -> {
            try {
                doRun();
            } catch (Exception e) {
                logger.debug("Exception occurred in Query Sandbox service", e);
            }
        }, sandboxServiceSettings.getRunIntervalMillis(), ThreadPool.Names.GENERIC);
    }

    @Override
    protected void doStop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel();
        }
    }

    @Override
    protected void doClose() throws IOException {}
}
