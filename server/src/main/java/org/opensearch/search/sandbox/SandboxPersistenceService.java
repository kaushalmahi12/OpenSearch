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
import org.opensearch.action.sandbox.CreateSandboxResponse;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ClusterStateUpdateTask;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.service.ClusterManagerTaskThrottler;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Priority;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.rest.RestStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence Service for Sandbox objects
 * for now we are saving the objects in @link Metadata of @link ClusterState object
 */
public class SandboxPersistenceService implements Persistable {
    private static final Logger logger = LogManager.getLogger(SandboxPersistenceService.class);
    private final ClusterService clusterService;
    private static final String SOURCE = "sandbox-persistence-service";
    private static final String CREATE_SANDBOX_THROTTLING_KEY = "create-sandbox";
    final ClusterManagerTaskThrottler.ThrottlingKey createSandboxThrottlingKey;

    @Inject
    public SandboxPersistenceService(final ClusterService clusterService) {
        this.clusterService = clusterService;
        this.createSandboxThrottlingKey = clusterService.registerClusterManagerTask(CREATE_SANDBOX_THROTTLING_KEY, true);
    }

    public void persist(Sandbox sandbox, ActionListener<CreateSandboxResponse> listener) {
        persistInClusterStateMetadata(sandbox, listener);
    }

    void persistInClusterStateMetadata(Sandbox sandbox, ActionListener<CreateSandboxResponse> listener) {
        clusterService.submitStateUpdateTask(SOURCE, new ClusterStateUpdateTask(Priority.URGENT) {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return saveNewSandboxObjectInClusterState(sandbox, currentState);
            }

            @Override
            public ClusterManagerTaskThrottler.ThrottlingKey getClusterManagerThrottlingKey() {
                return createSandboxThrottlingKey;
            }

            @Override
            public void onFailure(String source, Exception e) {
                logger.warn("failed to save Sandbox object due to error: {}, for source: {}", e.getMessage(), source);
                CreateSandboxResponse response = new CreateSandboxResponse();
                response.setRestStatus(RestStatus.FAILED_DEPENDENCY);
                listener.onFailure(e);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                CreateSandboxResponse response = new CreateSandboxResponse(sandbox);
                response.setRestStatus(RestStatus.OK);
                listener.onResponse(response);
            }

            //TODO: create the hook to call the listener if needed once the cluster state is published
        });
    }

    /**
     * This method will be executed before we submit the new cluster state
     * @param sandbox
     * @param currentClusterState
     * @return
     */
    private ClusterState saveNewSandboxObjectInClusterState(final Sandbox sandbox, final ClusterState currentClusterState) {
        // TODO: add checks whether this already entails any existing sandboxes
        final Metadata metadata = currentClusterState.metadata();
        final List<Sandbox> previousSandboxes = metadata.getSandboxes();
        final List<Sandbox> newSandboxes = new ArrayList<>(previousSandboxes);
        newSandboxes.add(sandbox);
        return ClusterState.builder(currentClusterState)
            .metadata(
                Metadata.builder(metadata).sandboxes(newSandboxes).build()
            ).build();
    }
}
