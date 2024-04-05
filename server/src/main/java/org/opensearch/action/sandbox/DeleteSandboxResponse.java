/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.sandbox.Sandbox;

import java.io.IOException;
import java.util.List;

/**
 * Response object for the DeleteSandbox action
 */
public class DeleteSandboxResponse extends ActionResponse implements ToXContent, StatusToXContentObject {
    private final List<Sandbox> sandboxes;
    private RestStatus restStatus;

    public DeleteSandboxResponse() {
        this.sandboxes = null;
    }

    public DeleteSandboxResponse(StreamInput in) throws IOException {
        this.sandboxes = in.readList(Sandbox::new);
    }

    public DeleteSandboxResponse(List<Sandbox> sandboxes) {
        this.sandboxes = sandboxes;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(sandboxes);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray("deleted");
        for (Sandbox sb : sandboxes) {
            sb.toXContent(builder, params);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public void setRestStatus(RestStatus status) {
        this.restStatus = status;
    }

    @Override
    public RestStatus status() {
        return restStatus;
    }

    public List<Sandbox> getSandboxes() {
        return sandboxes;
    }
}
