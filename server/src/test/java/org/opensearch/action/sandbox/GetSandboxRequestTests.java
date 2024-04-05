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
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class GetSandboxRequestTests extends OpenSearchTestCase {
    public static final String NAME = "test_sandbox";

    public void testSerialization() throws IOException {
        GetSandboxRequest request = new GetSandboxRequest(NAME);
        assertEquals(NAME, request.getName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GetSandboxRequest otherRequest = new GetSandboxRequest(streamInput);
        assertEquals(request.getName(), otherRequest.getName());
    }

    public void testSerializationWithNull() throws IOException {
        GetSandboxRequest request = new GetSandboxRequest((String) null);
        assertEquals(null, request.getName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GetSandboxRequest otherRequest = new GetSandboxRequest(streamInput);
        assertEquals(request.getName(), otherRequest.getName());
    }
}
