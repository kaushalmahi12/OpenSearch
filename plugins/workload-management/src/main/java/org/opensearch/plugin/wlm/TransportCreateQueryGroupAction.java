/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.plugin.wlm.service.Persistable;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import static org.opensearch.cluster.metadata.QueryGroup.builder;

/**
 * Transport action for create QueryGroup
 *
 * @opensearch.internal
 */
public class TransportCreateQueryGroupAction extends HandledTransportAction<CreateQueryGroupRequest, CreateQueryGroupResponse> {

    private final ThreadPool threadPool;
    private final Persistable<QueryGroup> queryGroupPersistenceService;

    /**
     * Constructor for TransportCreateQueryGroupAction
     *
     * @param actionName - action name
     * @param transportService - a {@link TransportService} object
     * @param actionFilters - a {@link ActionFilters} object
     * @param threadPool - a {@link ThreadPool} object
     * @param queryGroupPersistenceService - a {@link Persistable} object
     */
    @Inject
    public TransportCreateQueryGroupAction(
        String actionName,
        TransportService transportService,
        ActionFilters actionFilters,
        ThreadPool threadPool,
        Persistable<QueryGroup> queryGroupPersistenceService
    ) {
        super(CreateQueryGroupAction.NAME, transportService, actionFilters, CreateQueryGroupRequest::new);
        this.threadPool = threadPool;
        this.queryGroupPersistenceService = queryGroupPersistenceService;
    }

    @Override
    protected void doExecute(Task task, CreateQueryGroupRequest request, ActionListener<CreateQueryGroupResponse> listener) {
        QueryGroup queryGroup = builder().name(request.getName())
            ._id(request.get_id())
            .mode(request.getMode().getName())
            .resourceLimits(request.getResourceLimits())
            .updatedAt(request.getUpdatedAtInMillis())
            .build();
        threadPool.executor(ThreadPool.Names.GENERIC).execute(() -> queryGroupPersistenceService.persist(queryGroup, listener));
    }
}
