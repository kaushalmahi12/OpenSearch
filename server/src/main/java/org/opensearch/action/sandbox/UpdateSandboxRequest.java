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
import org.opensearch.search.sandbox.Sandbox.SystemResource;

import java.io.IOException;

/**
 * Request class for UpdateSandbox action
 */
public class UpdateSandboxRequest extends ActionRequest implements Writeable.Reader<UpdateSandboxRequest> {
    String existingName;
    String updatingName;
    ResourceConsumptionLimits resourceConsumptionLimits;
    SandboxAttributes sandboxAttributes;
    String enforcement;

    public UpdateSandboxRequest(String existingName) {
        this.existingName = existingName;
    }

    public UpdateSandboxRequest(Sandbox sandbox) {
        this.updatingName = sandbox.getName();
        this.resourceConsumptionLimits = sandbox.getResourceConsumptionLimits();
        this.sandboxAttributes = sandbox.getSandboxAttributes();
        this.enforcement = sandbox.getEnforcement();
    }

    public UpdateSandboxRequest(StreamInput in) throws IOException {
        super(in);
        existingName = in.readOptionalString();
        updatingName = in.readOptionalString();
        SystemResource jvm = null;
        if (in.readBoolean()) {
            if (in.readBoolean()) {
                jvm = new SystemResource(in);
            }
            resourceConsumptionLimits = new ResourceConsumptionLimits(jvm);
        }
        if (in.readBoolean()) {
            sandboxAttributes = new SandboxAttributes(in);
        }
        enforcement = in.readOptionalString();
    }

    @Override
    public UpdateSandboxRequest read(StreamInput in) throws IOException {
        return new UpdateSandboxRequest(in);
    }

    public static UpdateSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser, true);
        return new UpdateSandboxRequest(sandbox);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getExistingName() {
        return existingName;
    }

    public void setExistingName(String existingName) {
        this.existingName = existingName;
    }

    public String getUpdatingName() {
        return updatingName;
    }

    public void setUpdatingName(String updatingName) {
        this.updatingName = updatingName;
    }

    public SandboxAttributes getSandboxAttributes() {
        return sandboxAttributes;
    }

    public void setSandboxAttributes(SandboxAttributes sandboxAttributes) {
        this.sandboxAttributes = sandboxAttributes;
    }

    public String getEnforcement() {
        return enforcement;
    }

    public void setEnforcement(String enforcement) {
        this.enforcement = enforcement;
    }

    public ResourceConsumptionLimits getResourceConsumptionLimits() {
        return resourceConsumptionLimits;
    }

    public void setResourceConsumptionLimits(ResourceConsumptionLimits resourceConsumptionLimits) {
        this.resourceConsumptionLimits = resourceConsumptionLimits;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(existingName);
        out.writeOptionalString(updatingName);
        if (resourceConsumptionLimits == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            if (resourceConsumptionLimits.getJvm() == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                resourceConsumptionLimits.getJvm().writeTo(out);
            }
        }
        if (sandboxAttributes == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            sandboxAttributes.writeTo(out);
        }
        out.writeOptionalString(enforcement);
    }
}
