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
import static org.opensearch.search.sandbox.SandboxTests.JVM;
import static org.opensearch.search.sandbox.SandboxTests.MONITOR;
import static org.opensearch.search.sandbox.SandboxTests.NAME_ONE;
import static org.opensearch.search.sandbox.SandboxTests.createSandbox;

public class CreateSandboxRequestTests extends OpenSearchTestCase {

    public void testSerialization() throws IOException {
        List<Double> resourceLimits = List.of(FORTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        assertEquals(NAME_ONE, sb.getName());
        CreateSandboxRequest request = new CreateSandboxRequest(sb);
        assertEquals(NAME_ONE, request.getName());
        assertEquals(MONITOR, request.getEnforcement());
        assertEquals(FORTY, request.getResourceConsumptionLimits().getJvm().getAllocation(), 0);
        assertEquals(JVM, request.getResourceConsumptionLimits().getJvm().getName());
        assertEquals(INDICES_NAME_VAL_ONE, request.getSandboxAttributes().getIndicesValues());

        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        CreateSandboxRequest otherRequest = new CreateSandboxRequest(streamInput);
        assertEquals(request.getName(), otherRequest.getName());
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
}
