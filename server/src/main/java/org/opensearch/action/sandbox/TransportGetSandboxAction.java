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
 * Transport level Action for GetSandboxAction
 */
public class TransportGetSandboxAction extends HandledTransportAction<GetSandboxRequest, GetSandboxResponse> {
    private final ThreadPool threadPool;
    private final Persistable<Sandbox> sandboxPersistenceService;

    @Inject
    public TransportGetSandboxAction(String actionName, TransportService transportService,
                                     ActionFilters actionFilters, ThreadPool threadPool,
                                     Persistable<Sandbox> sandboxPersistenceService) {
        super(GetSandboxAction.NAME, transportService, actionFilters, GetSandboxRequest::new);
        this.threadPool = threadPool;
        this.sandboxPersistenceService = sandboxPersistenceService;
    }

    @Override
    protected void doExecute(Task task, GetSandboxRequest request, ActionListener<GetSandboxResponse> listener) {
        String name = request.getName();
        threadPool.executor(ThreadPool.Names.GENERIC).execute(
            () ->
                sandboxPersistenceService.get(name, listener)
        );
    }
}
