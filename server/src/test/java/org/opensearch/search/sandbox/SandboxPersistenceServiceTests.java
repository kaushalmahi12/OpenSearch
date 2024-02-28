/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.junit.Before;
import org.opensearch.Version;
import org.opensearch.action.sandbox.CreateSandboxResponse;
import org.opensearch.action.sandbox.UpdateSandboxRequest;
import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.AbstractScopedSettings;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsModule;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.index.IndexSettings;
import org.opensearch.rest.action.RestStatusToXContentListener;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.opensearch.search.sandbox.SandboxPersistenceService.MAX_SANDBOX_COUNT;

public class SandboxPersistenceServiceTests extends OpenSearchTestCase {
    public static final String ID_ONE = "15026955478";
    public static final String ID_TWO = "15587005607";
    public static final String PARENT_ID = "15050655478";
    public static final int PRIORITY = 2;
    public static final double THIRTY = 30.0;
    public static final double FORTY = 40.0;
    public static final double FIFTY = 50.0;
    public static final double SIXTY = 60.0;
    public static final String ATTRIBUTE_ONE = "attributeOne";
    public static final String ATTRIBUTE_ONE_VAL = "attributeOneVal";
    public static final String ATTRIBUTE_TWO = "attributeTwo";
    public static final String ATTRIBUTE_TWO_VAL = "attributeTwoVal";

    public static final String SANDBOX_MAX_SETTING_NAME = "node.sandbox.max_count";



    public static Sandbox createSandbox(List<String> attributes, List<Double> resourceLimits, String _id) {
        List<Sandbox.SelectionAttribute> selectionAttributesList = new ArrayList<>();
        for (int i = 0; i < attributes.size()/2; i++) {
            selectionAttributesList.add(new Sandbox.SelectionAttribute(attributes.get(i*2), attributes.get(i*2 + 1)));
        }
        Sandbox.SystemResource jvm = Sandbox.SystemResource.builder().low(resourceLimits.get(0)).high(resourceLimits.get(1)).name("jvm").build();
        Sandbox.SystemResource cpu = Sandbox.SystemResource.builder().low(resourceLimits.get(2)).high(resourceLimits.get(3)).name("cpu").build();
        Sandbox.ResourceConsumptionLimits limit = new Sandbox.ResourceConsumptionLimits(jvm, cpu);
        Sandbox sandbox = Sandbox
            .builder()
            .id(_id)
            .selectionAttributes(selectionAttributesList)
            .resourceConsumptionLimit(limit)
            .tags(new ArrayList<String>())
            .parentId(PARENT_ID)
            .priority(PRIORITY)
            .build(true);
        return sandbox;
    }

    public void compareSandboxes(Sandbox responseSandbox, Sandbox otherResponseSandbox) {
        assertEquals(responseSandbox.get_id(), otherResponseSandbox.get_id());
        assertEquals(responseSandbox.getParentId(), otherResponseSandbox.getParentId());
        assertEquals(responseSandbox.getPriority(), otherResponseSandbox.getPriority());
        assertEquals(responseSandbox.getSelectionAttributes().size(), otherResponseSandbox.getSelectionAttributes().size());
        for (int j = 0; j < responseSandbox.getSelectionAttributes().size() / 2; j++) {
            assertEquals(responseSandbox.getSelectionAttributes().get(j).getAttributeNane(), responseSandbox.getSelectionAttributes().get(j).getAttributeNane());
            assertEquals(responseSandbox.getSelectionAttributes().get(j).getAttributeValuePrefix(), responseSandbox.getSelectionAttributes().get(j).getAttributeValuePrefix());
        }
        assertEquals(responseSandbox.getResourceConsumptionLimits().getCpu().getLow(), otherResponseSandbox.getResourceConsumptionLimits().getCpu().getLow(), 0);
        assertEquals(responseSandbox.getResourceConsumptionLimits().getCpu().getHigh(), otherResponseSandbox.getResourceConsumptionLimits().getCpu().getHigh(), 0);
        assertEquals(responseSandbox.getResourceConsumptionLimits().getJvm().getLow(), otherResponseSandbox.getResourceConsumptionLimits().getJvm().getLow(), 0);
        assertEquals(responseSandbox.getResourceConsumptionLimits().getJvm().getHigh(), otherResponseSandbox.getResourceConsumptionLimits().getJvm().getHigh(), 0);
    }

    public ClusterState createClusterState() {
        // create current clusterState, which consists of 2 sandboxes
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        List<Sandbox> list = new ArrayList<>();
        list.add(createSandbox(attributes, resourceLimits, ID_ONE));
        resourceLimits = List.of(THIRTY, SIXTY, FIFTY, SIXTY);
        attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL);
        list.add(createSandbox(attributes, resourceLimits, ID_TWO));
        Metadata metadata = Metadata.builder().sandboxes(list).build();
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        return clusterState;
    }

    public void testGetSingleSandbox() {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(mock(ClusterService.class), settings, clusterSettings);
        ClusterState clusterState = createClusterState();
        List<Sandbox> sb = sandboxPersistenceService.getFromClusterStateMetadata(ID_ONE, clusterState);
        assertEquals(1, sb.size());
        Sandbox sandbox = sb.get(0);
        assertEquals(ID_ONE, sandbox.get_id());
        assertEquals(PARENT_ID, sandbox.getParentId());
        assertEquals(PRIORITY, (int) sandbox.getPriority());
        assertEquals(2, sandbox.getSelectionAttributes().size());
        assertEquals(FORTY, sandbox.getResourceConsumptionLimits().getCpu().getLow(), 0);
    }

    public void testGetAllSandboxes() {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        List<Sandbox> res = sandboxPersistenceService.getFromClusterStateMetadata(null, clusterState);
        assertEquals(2, res.size());
        Set<String> currentID = res.stream().map((sb)->(sb.get_id())).collect(Collectors.toSet());
        assertTrue(currentID.contains(ID_ONE));
        assertTrue(currentID.contains(ID_TWO));
    }

    public void testGetZeroSandboxes() {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(mock(ClusterService.class), settings, clusterSettings);
        ClusterState clusterState = createClusterState();
        List<Sandbox> res = sandboxPersistenceService.getFromClusterStateMetadata(PARENT_ID, clusterState);
        assertEquals(0, res.size());
    }

    public void testDeleteSingleSandbox() throws IOException {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        List<Sandbox> oldSandboxes = clusterState.getMetadata().getSandboxes();
        ClusterState newClusterState = sandboxPersistenceService.deleteNewSandboxObjectInClusterState(ID_TWO, clusterState);
        List<Sandbox> sandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, sandboxes.size());
        Sandbox sandbox = sandboxes.get(0);
        Sandbox oldSandbox = oldSandboxes.stream().filter((s)->(s.get_id().equals(ID_ONE))).collect(Collectors.toList()).get(0);
        assertEquals(ID_ONE, sandbox.get_id());

        // make sure the serialization of sandboxes match
        BytesStreamOutput out = new BytesStreamOutput();
        sandbox.writeTo(out);
        BytesReference streamInput = out.bytes();

        BytesStreamOutput out1 = new BytesStreamOutput();
        oldSandbox.writeTo(out1);
        BytesReference streamInput1 = out1.bytes();

        assertEquals(streamInput.toString(), streamInput1.toString());
    }

    public void testDeleteAllSandboxes()  {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        ClusterState newClusterState = sandboxPersistenceService.deleteNewSandboxObjectInClusterState(null, clusterState);
        List<Sandbox> sandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(0, sandboxes.size());
    }

    public void testDeleteNonExistedSandbox()  {
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.deleteNewSandboxObjectInClusterState("34253647586", clusterState));
    }

    public void testUpdateSandbox()  {
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        Sandbox current = createSandbox(attributes, resourceLimits, ID_ONE);
        List<Sandbox> list = new ArrayList<>();
        list.add(current);
        resourceLimits = List.of(THIRTY, SIXTY, FIFTY, SIXTY);
        attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_TWO_VAL);
        Sandbox updated = createSandbox(attributes, resourceLimits, ID_TWO);
        Metadata metadata = Metadata.builder().sandboxes(list).build();
        Settings settings = Settings.builder()
            .put(SANDBOX_MAX_SETTING_NAME, 5)
            .build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        ClusterState newClusterState = sandboxPersistenceService.updateSandboxObjectInClusterState(current, updated, clusterState);
        List<Sandbox> updatedSandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, updatedSandboxes.size());
        compareSandboxes(updated, updatedSandboxes.get(0));
    }
 }
