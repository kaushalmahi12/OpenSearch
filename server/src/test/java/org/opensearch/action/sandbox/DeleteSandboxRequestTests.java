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

public class DeleteSandboxRequestTests extends OpenSearchTestCase {
    public static final String ID = "1502695575";
    public void testSerialization() throws IOException {
        DeleteSandboxRequest request = new DeleteSandboxRequest(ID);
        assertEquals(ID, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        DeleteSandboxRequest otherRequest = new DeleteSandboxRequest(streamInput);
        assertEquals(request.get_id(), otherRequest.get_id());
    }

    public void testSerializationWithNull() throws IOException {
        DeleteSandboxRequest request = new DeleteSandboxRequest((String) null);
        assertEquals(null, request.get_id());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        DeleteSandboxRequest otherRequest = new DeleteSandboxRequest(streamInput);
        assertEquals(request.get_id(), otherRequest.get_id());
    }
}
