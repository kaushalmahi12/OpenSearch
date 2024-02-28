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

public class UpdateSandboxRequestTests extends OpenSearchTestCase {
    public static final String ID = "1502695575";
    public static final double THIRTY = 30.0;
    public static final double FORTY = 40.0;
    public static final double SIXTY = 60.0;
    public static final String ATTRIBUTE_ONE = "attributeOne";
    public static final String ATTRIBUTE_ONE_VAL = "attributeOneVal";
    public static final String ATTRIBUTE_TWO = "attributeTwo";
    public static final String ATTRIBUTE_TWO_VAL = "attributeTwoVal";
    public void testSerialization() throws IOException {
        List<Double> resourceLimits = List.of(THIRTY, SIXTY, FORTY, SIXTY);
        List<String> attributes = List.of(ATTRIBUTE_ONE, ATTRIBUTE_ONE_VAL, ATTRIBUTE_TWO, ATTRIBUTE_TWO_VAL);
        Sandbox sb = SandboxPersistenceServiceTests.createSandbox(attributes, resourceLimits, ID);
        assertEquals(ID, sb.get_id());
        UpdateSandboxRequest request = new UpdateSandboxRequest(sb);
        assertEquals(ID, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        UpdateSandboxRequest otherRequest = new UpdateSandboxRequest(streamInput);
        assertEquals(request.get_id(), otherRequest.get_id());
    }

    public void testSerializationOnlyID() throws IOException {
        UpdateSandboxRequest request = new UpdateSandboxRequest(ID);
        assertEquals(ID, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        UpdateSandboxRequest otherRequest = new UpdateSandboxRequest(streamInput);
        assertEquals(ID, otherRequest.get_id());
    }
}
