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
 * Request class for UpdateSandbox action
 */
public class UpdateSandboxRequest extends ActionRequest implements Writeable.Reader<UpdateSandboxRequest> {
    String _id;
    String parentSandboxId;
    Integer priority;
    ResourceConsumptionLimits resourceConsumptionLimits;
    List<SelectionAttribute> selectionAttributes;
    List<String> tags;
    public UpdateSandboxRequest(String _id) {
        this._id = _id;
    }

    public UpdateSandboxRequest(Sandbox sandbox) {
        this._id = sandbox.get_id();
        this.parentSandboxId = sandbox.getParentId();
        this.resourceConsumptionLimits = sandbox.getResourceConsumptionLimits();
        this.selectionAttributes = sandbox.getSelectionAttributes();
        this.tags = sandbox.getTags();
        this.priority = sandbox.getPriority();
    }

    public UpdateSandboxRequest(StreamInput in) throws IOException {
        super(in);
        _id = in.readString();
        parentSandboxId = in.readOptionalString();
        priority = in.readOptionalVInt();
        Sandbox.SystemResource jvm = null, cpu = null;
        if (in.readBoolean()) {
            if (in.readBoolean()) {
                jvm = new Sandbox.SystemResource(in);
            }
            if (in.readBoolean()) {
                cpu = new Sandbox.SystemResource(in);
            }
            resourceConsumptionLimits = new ResourceConsumptionLimits(jvm, cpu);
        }
        if (in.readBoolean()) {
            selectionAttributes = in.readList(SelectionAttribute::new);
        }
        if (in.readBoolean()) {
            int tagsLength = in.readVInt();
            tags = new ArrayList<>(tagsLength);
            for (int i=0; i<tagsLength; i++) {
                tags.add(in.readString());
            }
        }
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

    public String getParentSandboxId() {
        return parentSandboxId;
    }

    public Integer getPriority() {
        return priority;
    }

    public ResourceConsumptionLimits getResourceConsumptionLimits() {
        return resourceConsumptionLimits;
    }

    public List<SelectionAttribute> getSelectionAttributes() {
        return selectionAttributes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setParentSandboxId(String parentSandboxId) {
        this.parentSandboxId = parentSandboxId;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setResourceConsumptionLimits(ResourceConsumptionLimits resourceConsumptionLimits) {
        this.resourceConsumptionLimits = resourceConsumptionLimits;
    }

    public void setSelectionAttributes(List<SelectionAttribute> selectionAttributes) {
        this.selectionAttributes = selectionAttributes;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(_id);
        out.writeOptionalString(parentSandboxId);
        out.writeOptionalVInt(priority);
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
            if (resourceConsumptionLimits.getCpu() == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                resourceConsumptionLimits.getCpu().writeTo(out);
            }
        }
        if (selectionAttributes == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeList(selectionAttributes);
        }
        if (tags == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeVInt(tags.size());
            for (String tag: tags) {
                out.writeString(tag);
            };
        }
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }
}
