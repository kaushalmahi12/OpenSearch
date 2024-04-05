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
import static org.opensearch.search.sandbox.SandboxTests.compareSandboxes;
import static org.opensearch.search.sandbox.SandboxTests.createSandbox;

public class CreateSandboxResponseTests extends OpenSearchTestCase {

    public void testSerializationSingleSandbox() throws IOException {
        List<Double> resourceLimits = List.of(FORTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        Sandbox sb = createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR);
        CreateSandboxResponse response = new CreateSandboxResponse(sb);
        compareSandboxes(List.of(sb), List.of(response.getSandbox()));

        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        CreateSandboxResponse otherResponse = new CreateSandboxResponse(streamInput);
        assertEquals(response.status(), otherResponse.status());
        compareSandboxes(List.of(response.getSandbox()), List.of(otherResponse.getSandbox()));
    }
}
