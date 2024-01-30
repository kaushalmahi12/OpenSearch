/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.search.sandbox.Persistable;
import org.opensearch.search.sandbox.Sandbox;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

/**
 * Transport level Action for DeleteSandboxAction
 */
public class TransportDeleteSandboxAction extends HandledTransportAction<DeleteSandboxRequest, DeleteSandboxResponse> {
    private final ThreadPool threadPool;
    private final Persistable<Sandbox> sandboxPersistenceService;

    @Inject
    public TransportDeleteSandboxAction(String actionName, TransportService transportService,
                                        ActionFilters actionFilters, ThreadPool threadPool,
                                        Persistable<Sandbox> sandboxPersistenceService) {
        super(DeleteSandboxAction.NAME, transportService, actionFilters, DeleteSandboxRequest::new);
        this.threadPool = threadPool;
        this.sandboxPersistenceService = sandboxPersistenceService;
    }

    @Override
    protected void doExecute(Task task, DeleteSandboxRequest request, ActionListener<DeleteSandboxResponse> listener) {
        String _id = request.get_id();
        threadPool.executor(ThreadPool.Names.GENERIC).execute(
            () ->
                sandboxPersistenceService.delete(_id, listener)
            );
    }
}
