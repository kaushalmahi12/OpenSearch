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
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.List;

import static org.opensearch.search.sandbox.SandboxTests.FORTY;
import static org.opensearch.search.sandbox.SandboxTests.INDICES_NAME_VAL_ONE;
import static org.opensearch.search.sandbox.SandboxTests.MONITOR;
import static org.opensearch.search.sandbox.SandboxTests.NAME_ONE;
import static org.opensearch.search.sandbox.SandboxTests.createSandbox;

public class UpdateSandboxRequestTests extends OpenSearchTestCase {

    public void testSerialization() throws IOException {
        List<Double> resourceLimits = List.of(FORTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        assertEquals(NAME_ONE, sb.getName());
        UpdateSandboxRequest request = new UpdateSandboxRequest(sb);
        assertEquals(NAME_ONE, request.getUpdatingName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        UpdateSandboxRequest otherRequest = new UpdateSandboxRequest(streamInput);
        assertEquals(request.getUpdatingName(), otherRequest.getUpdatingName());
        assertEquals(request.getExistingName(), otherRequest.getExistingName());
        assertEquals(request.getEnforcement(), otherRequest.getEnforcement());
        assertEquals(
            request.getResourceConsumptionLimits().getJvm().getAllocation(),
            otherRequest.getResourceConsumptionLimits().getJvm().getAllocation(),
            0
        );
        assertEquals(
            request.getResourceConsumptionLimits().getJvm().getName(),
            otherRequest.getResourceConsumptionLimits().getJvm().getName()
        );
        assertEquals(request.getSandboxAttributes().getIndicesValues(), otherRequest.getSandboxAttributes().getIndicesValues());
    }

    public void testSerializationOnlyName() throws IOException {
        UpdateSandboxRequest request = new UpdateSandboxRequest(NAME_ONE);
        assertEquals(NAME_ONE, request.getExistingName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        UpdateSandboxRequest otherRequest = new UpdateSandboxRequest(streamInput);
        assertEquals(NAME_ONE, otherRequest.getExistingName());
    }
}
