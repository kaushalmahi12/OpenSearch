/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.lifecycle.AbstractLifecycleComponent;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.Scheduler;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.Map;

public class QuerySandboxService extends AbstractLifecycleComponent {
    private static final Logger logger = LogManager.getLogger(QuerySandboxService.class);

    private SandboxedRequestTrackerService requestTrackerService;
    private RequestSandboxClassifier requestSandboxClassifier;
    private volatile Scheduler.Cancellable scheduledFuture;
    private QuerySandboxServiceSettings sandboxServiceSettings;
    private ThreadPool threadPool;

    public QuerySandboxService(
        SandboxedRequestTrackerService requestTrackerService,
        RequestSandboxClassifier requestSandboxClassifier,
        QuerySandboxServiceSettings sandboxServiceSettings,
        ThreadPool threadPool
    ) {
        this.requestTrackerService = requestTrackerService;
        this.requestSandboxClassifier = requestSandboxClassifier;
        this.sandboxServiceSettings = sandboxServiceSettings;
        this.threadPool = threadPool;
    }

    public void startTracking(final Task task) {
        final Sandbox taskSandbox = requestSandboxClassifier.classify(task, requestTrackerService.getAvailableSandboxes());
        requestTrackerService.startTracking(task, taskSandbox);
    }

    private void doRun() {
        requestTrackerService.updateSandboxResourceUsages();
        requestTrackerService.cancelViolatingTasks();
        requestTrackerService.pruneSandboxes();
    }


    @Override
    protected void doStart() {
        scheduledFuture = threadPool.scheduleWithFixedDelay(() -> {
                try {
                    doRun();
                } catch (Exception e) {
                    logger.debug("Exception occurred in Query Sandbox service", e);
                }
            },
            sandboxServiceSettings.getRunIntervalMillis(),
            ThreadPool.Names.GENERIC);
    }

    public boolean addSandbox(Sandbox sandbox) {
        return requestTrackerService.addSandbox(sandbox);
    }

    public boolean removeSandbox(Sandbox sandbox) {
        return requestTrackerService.removeSandbox(sandbox);
    }

    public boolean updateSandbox(Sandbox sandbox) {
        return requestTrackerService.updateSandbox(sandbox);
    }


    @Override
    protected void doStop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel();
        }
    }

    @Override
    protected void doClose() throws IOException { }

    public Map<String, SandboxStats.SandboxStatsHolder> stats() {
        return requestTrackerService.getSandboxLevelStats();
    }
}
