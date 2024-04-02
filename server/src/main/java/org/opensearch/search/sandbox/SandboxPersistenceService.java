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
import org.opensearch.action.sandbox.*;
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
import org.opensearch.tasks.Task;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Persistence Service for Sandbox objects
 * for now we are saving the objects in @link Metadata of @link ClusterState object
 */
public class SandboxPersistenceService implements Persistable<Sandbox> {
    private static final Logger logger = LogManager.getLogger(SandboxPersistenceService.class);
    private final ClusterService clusterService;
    private static final String SOURCE = "sandbox-persistence-service";
    private static final String CREATE_SANDBOX_THROTTLING_KEY = "create-sandbox";
    private static final String UPDATE_SANDBOX_THROTTLING_KEY = "update-sandbox";
    private static final String DELETE_SANDBOX_THROTTLING_KEY = "delete-sandbox";
    private static final String SANDBOX_COUNT_SETTING_NAME = "node.sandbox.max_count";

    private static AtomicInteger inflightCreateSandboxRequestCount;
    private static DoubleAdder inflightJVMTotal;
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
    final ClusterManagerTaskThrottler.ThrottlingKey updateSandboxThrottlingKey;
    final ClusterManagerTaskThrottler.ThrottlingKey deleteSandboxThrottlingKey;

    @Inject
    public SandboxPersistenceService(final ClusterService clusterService,
                                     final Settings settings,
                                     final ClusterSettings clusterSettings) {
        this.clusterService = clusterService;
        this.createSandboxThrottlingKey = clusterService.registerClusterManagerTask(CREATE_SANDBOX_THROTTLING_KEY, true);
        this.deleteSandboxThrottlingKey = clusterService.registerClusterManagerTask(DELETE_SANDBOX_THROTTLING_KEY, true);
        this.updateSandboxThrottlingKey = clusterService.registerClusterManagerTask(UPDATE_SANDBOX_THROTTLING_KEY, true);
        maxSandboxCount = MAX_SANDBOX_COUNT.get(settings);
        clusterSettings.addSettingsUpdateConsumer(MAX_SANDBOX_COUNT, this::setMaxSandboxCount);
        inflightCreateSandboxRequestCount = new AtomicInteger();
        inflightJVMTotal = new DoubleAdder();
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
                inflightJVMTotal.add(-sandbox.getResourceConsumptionLimits().getJvm().getAllocation());
                logger.warn("failed to save Sandbox object due to error: {}, for source: {}", e.getMessage(), source);
                CreateSandboxResponse response = new CreateSandboxResponse();
                response.setRestStatus(RestStatus.FAILED_DEPENDENCY);
                listener.onFailure(e);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                inflightCreateSandboxRequestCount.decrementAndGet();
                inflightJVMTotal.add(-sandbox.getResourceConsumptionLimits().getJvm().getAllocation());
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

        String sandboxName = sandbox.getName();
        if (previousSandboxes.stream().anyMatch(prevSandbox -> prevSandbox.getName().equals(sandboxName))) {
            logger.warn("Sandbox with name {} already exists. Not creating a new one.", sandboxName);
            throw new RuntimeException("Sandbox with name " + sandboxName + " already exists. Not creating a new one.");
        }

        if (isIdenticalToExistingAttributes(sandbox, previousSandboxes)) {
            logger.error("New sandbox attributes are identical to one of the existing attributes. not creating a new sandbox.");
            throw new RuntimeException("New sandbox attributes are identical to one of the existing attributes. not creating a new sandbox.");
        }

        double currentJVMUsage = 0;
        for (Sandbox existingSandbox: previousSandboxes) {
            currentJVMUsage += existingSandbox.getResourceConsumptionLimits().getJvm().getAllocation();
        }
        double newJVMUsage = sandbox.getResourceConsumptionLimits().getJvm().getAllocation();
        inflightJVMTotal.add(newJVMUsage);
        double totalJVMUsage = currentJVMUsage + inflightJVMTotal.doubleValue();
        if (inflightCreateSandboxRequestCount.incrementAndGet() + previousSandboxes.size() > maxSandboxCount) {
            logger.error("{} value exceeded its assigned limit of {}", SANDBOX_COUNT_SETTING_NAME, maxSandboxCount);
            throw new RuntimeException("Can't create more than " + maxSandboxCount + " sandboxes in the system");
        }
        if (totalJVMUsage > 1) {
            logger.error("Total JVM allocation will become {} after adding this sandbox, which goes above the max limit of 1.0", totalJVMUsage);
            throw new RuntimeException("Total JVM allocation will become " + totalJVMUsage + " after adding this sandbox, which goes above the max limit of 1.0");
        }

        newSandboxes.add(sandbox);
        return ClusterState.builder(currentClusterState)
            .metadata(
                Metadata.builder(metadata).sandboxes(newSandboxes).build()
            ).build();
    }

    private boolean isIdenticalToExistingAttributes(final Sandbox sandbox, final List<Sandbox> existingSandboxes) {
        String sandboxIndicesValues = sandbox.getSandboxAttributes().getIndicesValues();
        HashSet<String> sandboxIndicesValuesSet = new HashSet<>(Arrays.asList(sandboxIndicesValues.split(",")));

        for (Sandbox existingSandbox: existingSandboxes) {
            String existingIndicesValues  = existingSandbox.getSandboxAttributes().getIndicesValues();
            HashSet<String> existingIndicesValuesSet = new HashSet<>(Arrays.asList(existingIndicesValues.split(",")));
            if (sandboxIndicesValuesSet.equals(existingIndicesValuesSet)) {
                return true;
            }
        }

        return false;
    }

    public <U extends ActionResponse> void update(Sandbox sandbox, ActionListener<U> listener) {
        String _id = sandbox.get_id();
        ClusterState currentState = clusterService.state();
        List<Sandbox> currentSandboxes = currentState.getMetadata().getSandboxes();
        List<Sandbox> targetSandboxList = currentSandboxes.stream()
            .filter((sb)->(sb.get_id().equals(_id)))
            .collect(Collectors.toList());
        if (targetSandboxList.isEmpty()) {
            logger.warn("No sandbox exists with the provided _id: {}", _id);
            Exception e = new RuntimeException(String.format("No sandbox exists with the provided _id: %s", _id));
            UpdateSandboxResponse response = new UpdateSandboxResponse();
            response.setRestStatus(RestStatus.NOT_FOUND);
            listener.onFailure(e);
            return;
        }
        Sandbox targetSandbox = targetSandboxList.get(0);

        // build the sandbox with updated fields
        Integer priority = sandbox.priority == null? targetSandbox.getPriority() : sandbox.getPriority();
        String parentId = sandbox.parentId == null? targetSandbox.getParentId():sandbox.getParentId();
        List<Sandbox.SelectionAttribute> selectionAttributes = new ArrayList<>();
        if (sandbox.getSelectionAttributes() == null || sandbox.getSelectionAttributes().isEmpty()) {
            selectionAttributes = targetSandbox.getSelectionAttributes();
        } else {
            Map<String, Sandbox.SelectionAttribute> attributeMap = new HashMap<>();
            targetSandbox.getSelectionAttributes()
                .forEach(attribute -> attributeMap.put(attribute.getAttributeNane(), attribute));
            sandbox.getSelectionAttributes()
                .forEach(attribute -> attributeMap.put(attribute.getAttributeNane(), attribute));
            selectionAttributes.addAll(attributeMap.values());
        }

        Sandbox.ResourceConsumptionLimits resourceConsumptionLimits;
        if (sandbox.getResourceConsumptionLimits() == null) {
            resourceConsumptionLimits = targetSandbox.getResourceConsumptionLimits();
        } else {
            Sandbox.SystemResource cpu = targetSandbox.getResourceConsumptionLimits().getCpu();
            Sandbox.SystemResource jvm = targetSandbox.getResourceConsumptionLimits().getJvm();
            if (sandbox.getResourceConsumptionLimits().getCpu() != null) {
                cpu = Sandbox.SystemResource.builder().name("cpu")
                    .high(sandbox.getResourceConsumptionLimits().getCpu().getHigh())
                    .low(sandbox.getResourceConsumptionLimits().getCpu().getLow())
                    .build();
            }
            if (sandbox.getResourceConsumptionLimits().getJvm() != null) {
                jvm = Sandbox.SystemResource.builder().name("jvm")
                    .high(sandbox.getResourceConsumptionLimits().getJvm().getHigh())
                    .low(sandbox.getResourceConsumptionLimits().getJvm().getLow())
                    .build();
            }
            resourceConsumptionLimits = new Sandbox.ResourceConsumptionLimits(jvm, cpu);
        }

        Sandbox updatedSandbox = Sandbox
            .builder()
            .id(_id)
            .selectionAttributes(selectionAttributes)
            .resourceConsumptionLimit(resourceConsumptionLimits)
            .tags(targetSandbox.getTags())
            .parentId(parentId)
            .priority(priority)
            .build(true);

        updateInClusterStateMetadata(targetSandbox, updatedSandbox, (ActionListener<UpdateSandboxResponse>) listener);
    }

    void updateInClusterStateMetadata(Sandbox currentSandbox, Sandbox updatedSandbox, ActionListener<UpdateSandboxResponse> listener) {
        clusterService.submitStateUpdateTask(SOURCE, new ClusterStateUpdateTask(Priority.URGENT) {
            @Override
            public ClusterState execute(ClusterState currentState) {
                return updateSandboxObjectInClusterState(currentSandbox, updatedSandbox, currentState);
            }

            @Override
            public ClusterManagerTaskThrottler.ThrottlingKey getClusterManagerThrottlingKey() {
                return updateSandboxThrottlingKey;
            }

            @Override
            public void onFailure(String source, Exception e) {
                logger.warn("failed to update Sandbox object due to error: {}, for source: {}", e.getMessage(), source);
                UpdateSandboxResponse response = new UpdateSandboxResponse();
                response.setRestStatus(RestStatus.FAILED_DEPENDENCY);
                listener.onFailure(e);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                UpdateSandboxResponse response = new UpdateSandboxResponse(updatedSandbox);
                response.setRestStatus(RestStatus.OK);
                listener.onResponse(response);
            }
        });
    }

    public ClusterState updateSandboxObjectInClusterState(Sandbox currentSandbox, Sandbox updatedSandbox, ClusterState currentState) {
        final Metadata metadata = currentState.metadata();
        List<Sandbox> currentSandboxes = currentState.getMetadata().getSandboxes();
        currentSandboxes.remove(currentSandbox);
        currentSandboxes.add(updatedSandbox);
        return ClusterState.builder(currentState)
            .metadata(
                Metadata.builder(metadata).sandboxes(currentSandboxes).build()
            ).build();
    }

    public <U extends ActionResponse> void delete(String _id, ActionListener<U> listener) {
        deleteInClusterStateMetadata(_id, (ActionListener<DeleteSandboxResponse>) listener);
    }

    void deleteInClusterStateMetadata(String _id, ActionListener<DeleteSandboxResponse> listener) {
        clusterService.submitStateUpdateTask(SOURCE, new ClusterStateUpdateTask(Priority.URGENT) {
            @Override
            public ClusterState execute(ClusterState currentState) throws Exception {
                return deleteNewSandboxObjectInClusterState(_id, currentState);
            }

            @Override
            public ClusterManagerTaskThrottler.ThrottlingKey getClusterManagerThrottlingKey() {
                return deleteSandboxThrottlingKey;
            }

            @Override
            public void onFailure(String source, Exception e) {
                logger.warn("failed to delete Sandbox object due to error: {}, for source: {}", e.getMessage(), source);
                DeleteSandboxResponse response = new DeleteSandboxResponse();
                response.setRestStatus(RestStatus.NOT_FOUND);
                listener.onFailure(e);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                final List<Sandbox> oldSandboxes = oldState.metadata().getSandboxes();
                final List<Sandbox> newSandboxes = newState.metadata().getSandboxes();
                final List<Sandbox> deletedSandboxes = oldSandboxes
                    .stream()
                    .filter(sb -> !newSandboxes.contains(sb))
                    .collect(Collectors.toList());
                DeleteSandboxResponse response = new DeleteSandboxResponse(deletedSandboxes);
                response.setRestStatus(RestStatus.OK);
                listener.onResponse(response);
            }
        });
    }

    ClusterState deleteNewSandboxObjectInClusterState(final String _id, final ClusterState currentClusterState) {
        final Metadata metadata = currentClusterState.metadata();
        final List<Sandbox> previousSandboxes = metadata.getSandboxes();
        final List<Sandbox> resultSandboxes;
        if (_id == null || _id.equals("")) {
            resultSandboxes = new ArrayList<>();
        } else {
            boolean sandboxWithIdExisted = previousSandboxes.stream().anyMatch(sb -> sb._id.equals(_id));
            if (!sandboxWithIdExisted) {
                logger.error("The sandbox with provided _id {} doesn't exist", _id);
                throw new RuntimeException(String.format("No sandbox exists with the provided _id: %s", _id));
            }
            resultSandboxes = previousSandboxes
                .stream()
                .filter(sb -> !sb._id.equals(_id))
                .collect(Collectors.toList());
        }
        return ClusterState.builder(currentClusterState)
            .metadata(
                Metadata.builder(metadata).sandboxes(resultSandboxes).build()
            ).build();
    }

    public <U extends ActionResponse> void get(String _id, ActionListener<U> listener) {
        ClusterState currentState = clusterService.state();
        List<Sandbox> currentSandboxes = currentState.getMetadata().getSandboxes();
        Set<String> currentID = currentSandboxes.stream().map((sb)->(sb.get_id())).collect(Collectors.toSet());
        if (_id != null && !currentID.contains(_id)) {
            logger.warn("No sandbox exists with the provided _id: {}", _id);
            Exception e = new RuntimeException(String.format("No sandbox exists with the provided _id: %s", _id));
            GetSandboxResponse response = new GetSandboxResponse();
            response.setRestStatus(RestStatus.NOT_FOUND);
            listener.onFailure(e);
            return;
        }
        List<Sandbox> resultSandboxes = getFromClusterStateMetadata(_id, currentState);
        GetSandboxResponse response = new GetSandboxResponse(resultSandboxes);
        response.setRestStatus(RestStatus.OK);
        listener.onResponse((U) response);
    }

    List<Sandbox> getFromClusterStateMetadata(String _id, ClusterState currentState) {
        List<Sandbox> currentSandboxes = currentState.getMetadata().getSandboxes();
        List<Sandbox> resultSandboxes = new ArrayList<>();
        if (_id == null || _id.equals("")) {
            resultSandboxes = currentSandboxes;
        } else {
            resultSandboxes.addAll(currentSandboxes.stream()
                .filter(sb -> sb.get_id().equals(_id))
                .collect(Collectors.toList()));
        }
        return resultSandboxes;
    }
}
