/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opensearch.search.sandbox.SandboxTests.FIFTY;
import static org.opensearch.search.sandbox.SandboxTests.FORTY;
import static org.opensearch.search.sandbox.SandboxTests.INDICES_NAME_VAL_ONE;
import static org.opensearch.search.sandbox.SandboxTests.INDICES_NAME_VAL_TWO;
import static org.opensearch.search.sandbox.SandboxTests.JVM;
import static org.opensearch.search.sandbox.SandboxTests.MONITOR;
import static org.opensearch.search.sandbox.SandboxTests.NAME_NONE_EXISTED;
import static org.opensearch.search.sandbox.SandboxTests.NAME_ONE;
import static org.opensearch.search.sandbox.SandboxTests.NAME_TWO;
import static org.opensearch.search.sandbox.SandboxTests.SIXTY;
import static org.opensearch.search.sandbox.SandboxTests.THIRTY;
import static org.opensearch.search.sandbox.SandboxTests.compareSandboxes;
import static org.opensearch.search.sandbox.SandboxTests.createSandbox;
import static org.mockito.Mockito.mock;

public class SandboxPersistenceServiceTests extends OpenSearchTestCase {
    public static final String SANDBOX_MAX_SETTING_NAME = "node.sandbox.max_count";

    public static ClusterState createClusterState() {
        // create current clusterState, which consists of 2 sandboxes
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        List<Sandbox> list = new ArrayList<>();
        list.add(createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR));
        resourceLimits = List.of(SIXTY);
        attributes = List.of(INDICES_NAME_VAL_TWO);
        list.add(createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR));
        Metadata metadata = Metadata.builder().sandboxes(list).build();
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        return clusterState;
    }

    public List<Object> prepareSandboxPersistenceService(List<Sandbox> sandboxes) {
        Metadata metadata = Metadata.builder().sandboxes(sandboxes).build();
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        return List.of(sandboxPersistenceService, clusterState);
    }

    public void testGetSingleSandbox() {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        List<Sandbox> sb = sandboxPersistenceService.getFromClusterStateMetadata(NAME_ONE, clusterState);
        assertEquals(1, sb.size());
        Sandbox sandbox = sb.get(0);
        assertEquals(NAME_ONE, sandbox.getName());
        assertEquals(MONITOR, sandbox.getEnforcement());
        assertEquals(THIRTY, sandbox.getResourceConsumptionLimits().getJvm().getAllocation(), 0);
        assertEquals(JVM, sandbox.getResourceConsumptionLimits().getJvm().getName());
        assertEquals(INDICES_NAME_VAL_ONE, sandbox.getSandboxAttributes().getIndicesValues());
    }

    public void testGetAllSandboxes() {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        List<Sandbox> res = sandboxPersistenceService.getFromClusterStateMetadata(null, clusterState);
        assertEquals(2, res.size());
        Set<String> currentNAME = res.stream().map(Sandbox::getName).collect(Collectors.toSet());
        assertTrue(currentNAME.contains(NAME_ONE));
        assertTrue(currentNAME.contains(NAME_TWO));
        compareSandboxes(clusterState.getMetadata().getSandboxes(), res);
    }

    public void testGetZeroSandboxes() {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(
            mock(ClusterService.class),
            settings,
            clusterSettings
        );
        ClusterState clusterState = createClusterState();
        List<Sandbox> res = sandboxPersistenceService.getFromClusterStateMetadata(NAME_NONE_EXISTED, clusterState);
        assertEquals(0, res.size());
    }

    public void testDeleteSingleSandbox() throws IOException {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        List<Sandbox> oldSandboxes = clusterState.getMetadata().getSandboxes();
        ClusterState newClusterState = sandboxPersistenceService.deleteNewSandboxObjectInClusterState(NAME_TWO, clusterState);
        List<Sandbox> sandbox = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, sandbox.size());
        List<Sandbox> oldSandbox = oldSandboxes.stream().filter((s) -> (s.getName().equals(NAME_ONE))).collect(Collectors.toList());
        compareSandboxes(sandbox, oldSandbox);
    }

    public void testDeleteAllSandboxes() {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        ClusterState newClusterState = sandboxPersistenceService.deleteNewSandboxObjectInClusterState(null, clusterState);
        List<Sandbox> sandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(0, sandboxes.size());
    }

    public void testDeleteNonExistedSandbox() {
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = createClusterState();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        assertThrows(
            RuntimeException.class,
            () -> sandboxPersistenceService.deleteNewSandboxObjectInClusterState(NAME_NONE_EXISTED, clusterState)
        );
    }

    public void testUpdateSandboxAllFields() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox current = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(current);
        resourceLimits = List.of(SIXTY);
        attributes = List.of(INDICES_NAME_VAL_TWO);
        Sandbox updated = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);

        ClusterState newClusterState = sandboxPersistenceService.updateSandboxObjectInClusterState(current, updated, clusterState);
        List<Sandbox> updatedSandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, updatedSandboxes.size());
        compareSandboxes(List.of(updated), updatedSandboxes);
    }

    public void testUpdateSandboxNameOnly() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox current = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(current);
        Sandbox updated = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);

        ClusterState newClusterState = sandboxPersistenceService.updateSandboxObjectInClusterState(current, updated, clusterState);
        List<Sandbox> updatedSandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, updatedSandboxes.size());
        compareSandboxes(List.of(updated), updatedSandboxes);
    }

    public void testCreateSandbox() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        List<Object> setup = prepareSandboxPersistenceService(new ArrayList<>());
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);

        ClusterState newClusterState = sandboxPersistenceService.saveNewSandboxObjectInClusterState(sb, clusterState);
        List<Sandbox> updatedSandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(1, updatedSandboxes.size());
        compareSandboxes(list, updatedSandboxes);
    }

    public void testCreateAnotherSandbox() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        resourceLimits = List.of(FORTY);
        attributes = List.of(INDICES_NAME_VAL_TWO);
        Sandbox sbTwo = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);

        ClusterState newClusterState = sandboxPersistenceService.saveNewSandboxObjectInClusterState(sbTwo, clusterState);
        List<Sandbox> updatedSandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(2, updatedSandboxes.size());
        compareSandboxes(List.of(sb, sbTwo), updatedSandboxes);
    }

    public void testCreateSandboxDuplicateName() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);

        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.saveNewSandboxObjectInClusterState(sb, clusterState));
    }

    public void testCreateSandboxDuplicateAttributes() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        resourceLimits = List.of(FORTY);
        attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sbTwo = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.saveNewSandboxObjectInClusterState(sbTwo, clusterState));
    }

    public void testCreateSandboxDuplicateAttributesWithComma() {
        List<Double> resourceLimits = List.of(THIRTY);
        List<String> attributes = List.of("one, two");
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        resourceLimits = List.of(FORTY);
        attributes = List.of("two, one");
        Sandbox sbTwo = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.saveNewSandboxObjectInClusterState(sbTwo, clusterState));
    }

    public void testCreateSandboxOverflowAllocation() {
        List<Double> resourceLimits = List.of(FIFTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        resourceLimits = List.of(SIXTY);
        attributes = List.of(INDICES_NAME_VAL_TWO);
        Sandbox sbTwo = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        List<Object> setup = prepareSandboxPersistenceService(list);
        SandboxPersistenceService sandboxPersistenceService = (SandboxPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.saveNewSandboxObjectInClusterState(sbTwo, clusterState));
    }

    public void testCreateSandboxOverflowCount() {
        List<Double> resourceLimits = List.of(FIFTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        List<Sandbox> list = new ArrayList<>();
        list.add(sb);

        resourceLimits = List.of(THIRTY);
        attributes = List.of(INDICES_NAME_VAL_TWO);
        Sandbox sbTwo = createSandbox(NAME_TWO, attributes, resourceLimits, MONITOR);

        Metadata metadata = Metadata.builder().sandboxes(list).build();
        Settings settings = Settings.builder().put(SANDBOX_MAX_SETTING_NAME, 1).build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        SandboxPersistenceService sandboxPersistenceService = new SandboxPersistenceService(clusterService, settings, clusterSettings);
        assertThrows(RuntimeException.class, () -> sandboxPersistenceService.saveNewSandboxObjectInClusterState(sbTwo, clusterState));
    }
}
