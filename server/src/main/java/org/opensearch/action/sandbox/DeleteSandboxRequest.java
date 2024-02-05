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

import java.io.IOException;

/**
 * Request class for DeleteSandbox action
 */
public class DeleteSandboxRequest extends ActionRequest implements Writeable.Reader<DeleteSandboxRequest> {
    String _id;

    public DeleteSandboxRequest(String _id) {
        this._id = _id;
    }

    public DeleteSandboxRequest(Sandbox sandbox) {
        this._id = sandbox.get_id();
    }

    public DeleteSandboxRequest(StreamInput in) throws IOException {
        super(in);
        this._id = in.readOptionalString();
    }

    @Override
    public DeleteSandboxRequest read(StreamInput in) throws IOException {
        return new DeleteSandboxRequest(in);
    }

    public static DeleteSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser, false);
        return new DeleteSandboxRequest(sandbox);
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
