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
 * Transport level Action for UpdateSandboxAction
 */
public class TransportUpdateSandboxAction extends HandledTransportAction<UpdateSandboxRequest, UpdateSandboxResponse> {
    private final ThreadPool threadPool;
    private final Persistable<Sandbox> sandboxPersistenceService;

    @Inject
    public TransportUpdateSandboxAction(String actionName, TransportService transportService,
                                        ActionFilters actionFilters, ThreadPool threadPool,
                                        Persistable<Sandbox> sandboxPersistenceService) {
        super(UpdateSandboxAction.NAME, transportService, actionFilters, UpdateSandboxRequest::new);
        this.threadPool = threadPool;
        this.sandboxPersistenceService = sandboxPersistenceService;
    }

    @Override
    protected void doExecute(Task task, UpdateSandboxRequest request, ActionListener<UpdateSandboxResponse> listener) {
        Sandbox sandbox;
        if (request.getPriority() == null) {
            sandbox = Sandbox
                .builder()
                .id(request.get_id())
                .selectionAttributes(request.getSelectionAttributes())
                .resourceConsumptionLimit(request.getResourceConsumptionLimits())
                .tags(request.getTags())
                .parentId(request.getParentSandboxId())
                .build(true);
        } else {
            sandbox = Sandbox
                .builder()
                .id(request.get_id())
                .selectionAttributes(request.getSelectionAttributes())
                .resourceConsumptionLimit(request.getResourceConsumptionLimits())
                .tags(request.getTags())
                .parentId(request.getParentSandboxId())
                .priority(request.getPriority())
                .build(true);
        }
        threadPool.executor(ThreadPool.Names.GENERIC).execute(
            () ->
                sandboxPersistenceService.update(sandbox, listener)
            );
    }
}
