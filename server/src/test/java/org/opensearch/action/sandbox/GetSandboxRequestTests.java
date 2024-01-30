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
    public static final String ID = "1502695575";
    public void testSerialization() throws IOException {
        GetSandboxRequest request = new GetSandboxRequest(ID);
        assertEquals(ID, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GetSandboxRequest otherRequest = new GetSandboxRequest(streamInput);
        assertEquals(request.get_id(), otherRequest.get_id());
    }

    public void testSerializationWithNull() throws IOException {
        GetSandboxRequest request = new GetSandboxRequest((String) null);
        assertEquals(null, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GetSandboxRequest otherRequest = new GetSandboxRequest(streamInput);
        assertEquals(request.get_id(), otherRequest.get_id());
    }
}
