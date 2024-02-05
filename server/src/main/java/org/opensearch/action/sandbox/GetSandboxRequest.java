/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.search.sandbox.Sandbox;
import org.opensearch.search.sandbox.Sandbox.ResourceConsumptionLimits;
import org.opensearch.search.sandbox.Sandbox.SelectionAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Request class for GetSandbox action
 */
public class GetSandboxRequest extends ActionRequest implements Writeable.Reader<GetSandboxRequest> {
    String _id;

    public GetSandboxRequest(String _id) {
        this._id = _id;
    }

    public GetSandboxRequest(Sandbox sandbox) {
        this._id = sandbox.get_id();
    }

    public GetSandboxRequest(StreamInput in) throws IOException {
        super(in);
        this._id = in.readOptionalString();
    }

    @Override
    public GetSandboxRequest read(StreamInput in) throws IOException {
        return new GetSandboxRequest(in);
    }

    public static GetSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser, false);
        return new GetSandboxRequest(sandbox);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(_id);
    }
}
