/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;

import java.io.IOException;

/**
 * A request for delete QueryGroup
 *
 * @opensearch.internal
 */
public class DeleteQueryGroupRequest extends ActionRequest implements Writeable.Reader<DeleteQueryGroupRequest> {
    String name;

    /**
     * Default constructor for DeleteQueryGroupRequest
     * @param name - name for the QueryGroup to get
     */
    public DeleteQueryGroupRequest(String name) {
        this.name = name;
    }

    /**
     * Constructor for DeleteQueryGroupRequest
     * @param in - A {@link StreamInput} object
     */
    public DeleteQueryGroupRequest(StreamInput in) throws IOException {
        super(in);
        name = in.readOptionalString();
    }

    @Override
    public DeleteQueryGroupRequest read(StreamInput in) throws IOException {
        return new DeleteQueryGroupRequest(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * Name getter
     */
    public String getName() {
        return name;
    }

    /**
     * Name setter
     * @param name - name to be set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(name);
    }
}
