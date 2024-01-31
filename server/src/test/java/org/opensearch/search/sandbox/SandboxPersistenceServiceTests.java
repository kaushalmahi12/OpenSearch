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
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SandboxPersistenceServiceTests extends OpenSearchTestCase {
    public static final String ID_ONE = "15026955478";
    public static final String ID_TWO = "15587005607";
    public static final String PARENT_ID = "15050655478";
    public static final int PRIORITY = 2;
    public static final double THIRTY = 30.0;
    public static final double FORTY = 40.0;
    public static final double FIFTY = 50.0;
    public static final double SIXTY = 50.0;
    public static final String ATTRIBUTE_ONE = "attributeOne";
    public static final String ATTRIBUTE_ONE_VAL = "attributeOneVal";
    public static final String ATTRIBUTE_TWO = "attributeTwo";
    public static final String ATTRIBUTE_TWO_VAL = "attributeTwoVal";

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
            .build();
        return sandbox;
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
        ClusterState clusterState = createClusterState();
        List<Sandbox> sb = SandboxPersistenceService.getFromClusterStateMetadata(ID_ONE, clusterState, null);
        assertEquals(1, sb.size());
        Sandbox sandbox = sb.get(0);
        assertEquals(ID_ONE, sandbox.get_id());
        assertEquals(PARENT_ID, sandbox.getParentId());
        assertEquals(PRIORITY, (int) sandbox.getPriority());
        assertEquals(2, sandbox.getSelectionAttributes().size());
        assertEquals(FORTY, sandbox.getResourceConsumptionLimits().getCpu().getLow(), 0);
    }

    public void testGetAllSandboxes() {
        ClusterState clusterState = createClusterState();
        List<Sandbox> res = SandboxPersistenceService.getFromClusterStateMetadata(null, clusterState, null);
        assertEquals(2, res.size());
        Set<String> currentID = res.stream().map((sb)->(sb.get_id())).collect(Collectors.toSet());
        assertTrue(currentID.contains(ID_ONE));
        assertTrue(currentID.contains(ID_TWO));
    }

    public void testGetZeroSandboxes() {
        ClusterState clusterState = createClusterState();
        List<Sandbox> res = SandboxPersistenceService.getFromClusterStateMetadata("53271890567", clusterState, null);
        assertEquals(0, res.size());
    }

    public void testDeleteSingleSandbox() throws IOException {
        ClusterState clusterState = createClusterState();
        List<Sandbox> oldSandboxes = clusterState.getMetadata().getSandboxes();
        ClusterState newClusterState = SandboxPersistenceService.deleteNewSandboxObjectInClusterState(ID_TWO, clusterState);
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
        ClusterState clusterState = createClusterState();
        List<Sandbox> oldSandboxes = clusterState.getMetadata().getSandboxes();
        ClusterState newClusterState = SandboxPersistenceService.deleteNewSandboxObjectInClusterState(null, clusterState);
        List<Sandbox> sandboxes = newClusterState.getMetadata().getSandboxes();
        assertEquals(0, sandboxes.size());
    }

    public void testDeleteNonExistedSandbox()  {
        ClusterState clusterState = createClusterState();
        List<Sandbox> oldSandboxes = clusterState.getMetadata().getSandboxes();
        assertThrows(RuntimeException.class, () -> SandboxPersistenceService.deleteNewSandboxObjectInClusterState("34253647586", clusterState));
    }

 }
