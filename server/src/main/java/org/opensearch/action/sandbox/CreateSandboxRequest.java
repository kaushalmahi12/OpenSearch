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
import org.opensearch.search.sandbox.Sandbox.SandboxAttributes;

import java.io.IOException;

/**
 * Request class for CreateSandbox action
 */
public class CreateSandboxRequest extends ActionRequest implements Writeable.Reader<CreateSandboxRequest> {
    String name;
    ResourceConsumptionLimits resourceConsumptionLimits;
    SandboxAttributes sandboxAttributes;
    String enforcement;

    public CreateSandboxRequest() {}

    public CreateSandboxRequest(Sandbox sandbox) {
        this.name = sandbox.getName();
        this.resourceConsumptionLimits = sandbox.getResourceConsumptionLimits();
        this.sandboxAttributes = sandbox.getSandboxAttributes();
        this.enforcement = sandbox.getEnforcement();
    }

    public CreateSandboxRequest(StreamInput in) throws IOException {
        super(in);
        name = in.readString();
        resourceConsumptionLimits = new ResourceConsumptionLimits(in);
        sandboxAttributes = new SandboxAttributes(in);
        enforcement = in.readString();
    }

    @Override
    public CreateSandboxRequest read(StreamInput in) throws IOException {
        return new CreateSandboxRequest(in);
    }

    public static CreateSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser, false);
        return new CreateSandboxRequest(sandbox);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getName() {
        return name;
    }

    public ResourceConsumptionLimits getResourceConsumptionLimits() {
        return resourceConsumptionLimits;
    }

    public SandboxAttributes getSandboxAttributes() {
        return sandboxAttributes;
    }

    public String getEnforcement() {
        return enforcement;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceConsumptionLimits(ResourceConsumptionLimits resourceConsumptionLimits) {
        this.resourceConsumptionLimits = resourceConsumptionLimits;
    }

    public void setSandboxAttributes(SandboxAttributes sandboxAttributes) {
        this.sandboxAttributes = sandboxAttributes;
    }

    public void setEnforcement(String enforcement) {
        this.enforcement = enforcement;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        Sandbox.writeToOutputStream(out, name, resourceConsumptionLimits, sandboxAttributes, enforcement);
    }
}
