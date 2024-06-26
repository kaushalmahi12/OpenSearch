/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class DeleteQueryGroupRequestTests extends OpenSearchTestCase {

    public void testSerialization() throws IOException {
        DeleteQueryGroupRequest request = new DeleteQueryGroupRequest(QueryGroupTestUtils.NAME_ONE);
        assertEquals(QueryGroupTestUtils.NAME_ONE, request.getName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        DeleteQueryGroupRequest otherRequest = new DeleteQueryGroupRequest(streamInput);
        assertEquals(request.getName(), otherRequest.getName());
    }

    public void testSerializationWithNull() throws IOException {
        DeleteQueryGroupRequest request = new DeleteQueryGroupRequest((String) null);
        assertNull(request.getName());
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        DeleteQueryGroupRequest otherRequest = new DeleteQueryGroupRequest(streamInput);
        assertEquals(request.getName(), otherRequest.getName());
    }
}
