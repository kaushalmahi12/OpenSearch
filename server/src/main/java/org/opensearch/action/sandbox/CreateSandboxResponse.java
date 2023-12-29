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

/**
 * Response object for the CreateSandbox action
 */
public class CreateSandboxResponse extends ActionResponse implements ToXContent, StatusToXContentObject {
    private final Sandbox sandbox;
    private RestStatus restStatus;

    public CreateSandboxResponse() {
        this.sandbox = null;
    }
    public CreateSandboxResponse(StreamInput in) throws IOException {
        sandbox = new Sandbox(in);
    }

    public CreateSandboxResponse(final Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        sandbox.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        sandbox.toXContent(builder, params);
        return builder;
    }

    public void setRestStatus(RestStatus status) {
        this.restStatus = status;
    }

    @Override
    public RestStatus status() {
        return restStatus;
    }
}
