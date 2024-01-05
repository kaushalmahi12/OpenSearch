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
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.rest.RestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persistence Service for Sandbox objects
 * for now we are saving the objects in @link Metadata of @link ClusterState object
 */
public class SandboxPersistenceService implements Persistable<Sandbox> {
    private static final Logger logger = LogManager.getLogger(SandboxPersistenceService.class);
    private final ClusterService clusterService;
    private static final String SOURCE = "sandbox-persistence-service";
    private static final String CREATE_SANDBOX_THROTTLING_KEY = "create-sandbox";
    private static final String SANDBOX_COUNT_SETTING_NAME = "node.sandbox.max_count";

    private static AtomicInteger inflightCreateSandboxRequestCount;
    private volatile int maxSandboxCount;
    public static final int DEFAULT_MAX_SANDBOX_COUNT_VALUE = 100;
    public static final Setting<Integer> MAX_SANDBOX_COUNT = Setting.intSetting(
        SANDBOX_COUNT_SETTING_NAME,
        DEFAULT_MAX_SANDBOX_COUNT_VALUE,
        0,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );
    final ClusterManagerTaskThrottler.ThrottlingKey createSandboxThrottlingKey;

    @Inject
    public SandboxPersistenceService(final ClusterService clusterService,
                                     final Settings settings,
                                     final ClusterSettings clusterSettings) {
        this.clusterService = clusterService;
        this.createSandboxThrottlingKey = clusterService.registerClusterManagerTask(CREATE_SANDBOX_THROTTLING_KEY, true);
        maxSandboxCount = MAX_SANDBOX_COUNT.get(settings);
        clusterSettings.addSettingsUpdateConsumer(MAX_SANDBOX_COUNT, this::setMaxSandboxCount);
        inflightCreateSandboxRequestCount = new AtomicInteger();
    }

    public void setMaxSandboxCount(int newMaxSandboxCount) {
        if (newMaxSandboxCount < 0) {
            throw new IllegalArgumentException("node.sandbox.max_count can't be negative");
        }
        this.maxSandboxCount = newMaxSandboxCount;
    }

    public <U extends ActionResponse> void persist(Sandbox sandbox, ActionListener<U> listener) {
        persistInClusterStateMetadata(sandbox, (ActionListener<CreateSandboxResponse>) listener);
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
                inflightCreateSandboxRequestCount.decrementAndGet();
                logger.warn("failed to save Sandbox object due to error: {}, for source: {}", e.getMessage(), source);
                CreateSandboxResponse response = new CreateSandboxResponse();
                response.setRestStatus(RestStatus.FAILED_DEPENDENCY);
                listener.onFailure(e);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                inflightCreateSandboxRequestCount.decrementAndGet();
                CreateSandboxResponse response = new CreateSandboxResponse(sandbox);
                response.setRestStatus(RestStatus.OK);
                listener.onResponse(response);
            }
        });
    }

    /**
     * This method will be executed before we submit the new cluster state
     * @param sandbox
     * @param currentClusterState
     * @return
     */
    private ClusterState saveNewSandboxObjectInClusterState(final Sandbox sandbox, final ClusterState currentClusterState) {
        final Metadata metadata = currentClusterState.metadata();
        final List<Sandbox> previousSandboxes = metadata.getSandboxes();
        final List<Sandbox> newSandboxes = new ArrayList<>(previousSandboxes);
        if (isAlreadyCoveredInExistingOnes(sandbox, previousSandboxes)) {
            // TODO: Should be throw an exception instead
            logger.warn("New sandbox is already covered in one of the existing sandboxes. not creating a new one..");
            return currentClusterState;
        }
        if (inflightCreateSandboxRequestCount.incrementAndGet() + previousSandboxes.size() > maxSandboxCount) {
            inflightCreateSandboxRequestCount.decrementAndGet();
            logger.error("{} value exceeded its assgined limit of {}", SANDBOX_COUNT_SETTING_NAME, maxSandboxCount);
            throw new RuntimeException("Can't create more than " + maxSandboxCount + " in the system");
        }
        newSandboxes.add(sandbox);
        return ClusterState.builder(currentClusterState)
            .metadata(
                Metadata.builder(metadata).sandboxes(newSandboxes).build()
            ).build();
    }

    private boolean isAlreadyCoveredInExistingOnes(final Sandbox sandbox, final List<Sandbox> existingSandboxes) {
        return existingSandboxes.stream().anyMatch(sandbox1 -> sandbox1.hasOvershadowingSelectionAttribute(sandbox));
    }
}
