/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.search.sandbox.Sandbox;
import org.opensearch.search.sandbox.SandboxPersistenceServiceTests;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetSandboxResponseTests extends OpenSearchTestCase {
    public static final String ID_ONE = "15026955478";
    public static final String ID_TWO = "150269568770";
    public static final double THIRTY = 30.0;
    public static final double FORTY = 40.0;
    public static final double FIFTY = 50.0;
    public static final double SIXTY = 60.0;
    public static final String ATTRIBUTE_ONE = "attributeOne";
    public static final String ATTRIBUTE_ONE_VAL = "attributeOneVal";
    public static final String ATTRIBUTE_TWO = "attributeTwo";
    public static final String ATTRIBUTE_TWO_VAL = "attributeTwoVal";

    public void compareSandboxes(List<Sandbox> listOne, List<Sandbox> listTwo) {
        assertEquals(listOne.size(), listTwo.size());
        for (int i = 0; i < listOne.size(); i++) {
            Sandbox responseSandbox = listOne.get(i);
            Sandbox otherResponseSandbox = listTwo.get(i);
            assertEquals(responseSandbox.get_id(), otherResponseSandbox.get_id());
            assertEquals(responseSandbox.getParentId(), otherResponseSandbox.getParentId());
            assertEquals(responseSandbox.getPriority(), otherResponseSandbox.getPriority());
            assertEquals(responseSandbox.getSelectionAttributes().size(), otherResponseSandbox.getSelectionAttributes().size());
            for (int j = 0; j < responseSandbox.getSelectionAttributes().size()/2; j++) {
                assertEquals(responseSandbox.getSelectionAttributes().get(j).getAttributeNane(), responseSandbox.getSelectionAttributes().get(j).getAttributeNane());
                assertEquals(responseSandbox.getSelectionAttributes().get(j).getAttributeValuePrefix(), responseSandbox.getSelectionAttributes().get(j).getAttributeValuePrefix());
            }
            assertEquals(responseSandbox.getResourceConsumptionLimits().getCpu().getLow(), otherResponseSandbox.getResourceConsumptionLimits().getCpu().getLow(), 0);
            assertEquals(responseSandbox.getResourceConsumptionLimits().getCpu().getHigh(), otherResponseSandbox.getResourceConsumptionLimits().getCpu().getHigh(), 0);
            assertEquals(responseSandbox.getResourceConsumptionLimits().getJvm().getLow(), otherResponseSandbox.getResourceConsumptionLimits().getJvm().getLow(), 0);
            assertEquals(responseSandbox.getResourceConsumptionLimits().getJvm().getHigh(), otherResponseSandbox.getResourceConsumptionLimits().getJvm().getHigh(), 0);
        }
    }

    public void testSerializationSingleSandbox() throws IOException {
        // create a list of sandboxes as the response
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        List<Sandbox> list = new ArrayList<>();
        list.add(SandboxPersistenceServiceTests.createSandbox(attributes, resourceLimits, ID_ONE));
        GetSandboxResponse response = new GetSandboxResponse(list);
        assertEquals(response.getSandboxes(), list);

        // serialize the response
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();

        // deserialize the response and check whether each field equals the original list of sandbox
        GetSandboxResponse otherResponse = new GetSandboxResponse(streamInput);
        assertEquals(response.status(), otherResponse.status());
        compareSandboxes(response.getSandboxes(), otherResponse.getSandboxes());
    }

    public void testSerializationMultipleSandbox() throws IOException {
        // create a list of sandboxes as the response
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        List<Sandbox> list = new ArrayList<>();
        list.add(SandboxPersistenceServiceTests.createSandbox(attributes, resourceLimits, ID_ONE));
        resourceLimits = List.of(THIRTY, SIXTY, FIFTY, SIXTY);
        attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL);
        list.add(SandboxPersistenceServiceTests.createSandbox(attributes, resourceLimits, ID_TWO));
        GetSandboxResponse response = new GetSandboxResponse(list);
        assertEquals(response.getSandboxes(), list);

        // serialize the response
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();

        // deserialize the response and check whether each field equals the original list of sandbox
        GetSandboxResponse otherResponse = new GetSandboxResponse(streamInput);
        assertEquals(2, otherResponse.getSandboxes().size());
        assertEquals(response.status(), otherResponse.status());
        compareSandboxes(response.getSandboxes(), otherResponse.getSandboxes());
    }

    public void testSerializationNull() throws IOException {
        // create a list of sandboxes (empty list) as the response
        List<Sandbox> list = new ArrayList<>();
        GetSandboxResponse response = new GetSandboxResponse(list);
        assertEquals(response.getSandboxes(), list);

        // serialize the response
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();

        // deserialize the response and check whether each field equals the original list of sandbox
        GetSandboxResponse otherResponse = new GetSandboxResponse(streamInput);
        assertEquals(response.status(), otherResponse.status());
        assertEquals(0, otherResponse.getSandboxes().size());
    }
}
