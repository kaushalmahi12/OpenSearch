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
import java.util.ArrayList;
import java.util.List;

import static org.opensearch.search.sandbox.SandboxPersistenceServiceTests.createClusterState;
import static org.opensearch.search.sandbox.SandboxTests.FORTY;
import static org.opensearch.search.sandbox.SandboxTests.INDICES_NAME_VAL_ONE;
import static org.opensearch.search.sandbox.SandboxTests.MONITOR;
import static org.opensearch.search.sandbox.SandboxTests.NAME_ONE;
import static org.opensearch.search.sandbox.SandboxTests.compareSandboxes;
import static org.opensearch.search.sandbox.SandboxTests.createSandbox;

public class GetSandboxResponseTests extends OpenSearchTestCase {

    public void testSerializationSingleSandbox() throws IOException {
        // create a list of sandboxes as the response
        List<Double> resourceLimits = List.of(FORTY);
        List<String> attributes = List.of(INDICES_NAME_VAL_ONE);
        List<Sandbox> list = new ArrayList<>();
        list.add(createSandbox(NAME_ONE, attributes, resourceLimits, MONITOR));
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
        List<Sandbox> list = createClusterState().getMetadata().getSandboxes();
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
