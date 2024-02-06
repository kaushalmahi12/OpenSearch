/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.cluster.service.ClusterManagerService;
import org.opensearch.common.lifecycle.AbstractLifecycleComponent;
import org.opensearch.tasks.Task;

import java.io.IOException;

public class SandboxService extends AbstractLifecycleComponent {

    private SandboxedRequestTrackerService requestTrackerService;
    private RequestSandboxClassifier requestSandboxClassifier;

    public void startTracking(final Task task) {
        final Sandbox taskSandbox = requestSandboxClassifier.classify(task, requestTrackerService.getAvailableSandboxes());
        requestTrackerService.startTracking(task, taskSandbox);
    }


    @Override
    protected void doStart() {
        requestTrackerService.updateSandboxResourceUsages();
        requestTrackerService.cancelViolatingTasks();
        requestTrackerService.pruneSandboxes();
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

    }

    @Override
    protected void doClose() throws IOException {

    }
}
