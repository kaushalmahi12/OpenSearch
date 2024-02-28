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

public class UpdateSandboxResponseTests extends OpenSearchTestCase {
    public static final String ID_ONE = "15026955478";
    public static final double THIRTY = 30.0;
    public static final double FORTY = 40.0;
    public static final double SIXTY = 60.0;
    public static final String ATTRIBUTE_ONE = "attributeOne";
    public static final String ATTRIBUTE_ONE_VAL = "attributeOneVal";
    public static final String ATTRIBUTE_TWO = "attributeTwo";
    public static final String ATTRIBUTE_TWO_VAL = "attributeTwoVal";

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

    public void testSerializationSingleSandbox() throws IOException {
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        Sandbox sb = SandboxPersistenceServiceTests.createSandbox(attributes, resourceLimits, ID_ONE);
        UpdateSandboxResponse response = new UpdateSandboxResponse(sb);
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        UpdateSandboxResponse otherResponse = new UpdateSandboxResponse(streamInput);
        assertEquals(response.status(), otherResponse.status());
        compareSandboxes(response.getSandbox(), otherResponse.getSandbox());
    }
}
