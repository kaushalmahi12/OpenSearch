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
 * Request class for CreateSandbox action
 */
public class CreateSandboxRequest extends ActionRequest implements Writeable.Reader<CreateSandboxRequest> {
    String parentSandboxId;
    Integer priority;
    ResourceConsumptionLimits resourceConsumptionLimits;
    List<SelectionAttribute> selectionAttributes;
    List<String> tags;
    public CreateSandboxRequest() {
//        tags = new ArrayList<>();
    }

    public CreateSandboxRequest(Sandbox sandbox) {
        this.parentSandboxId = sandbox.getParentId();
        this.resourceConsumptionLimits = sandbox.getResourceConsumptionLimits();
        this.selectionAttributes = sandbox.getSelectionAttributes();
        this.tags = sandbox.getTags();
        this.priority = sandbox.getPriority();
    }

    public CreateSandboxRequest(StreamInput in) throws IOException {
        super(in);
        parentSandboxId = in.readOptionalString();
        priority = in.readVInt();
        resourceConsumptionLimits = new ResourceConsumptionLimits(in);
        selectionAttributes = in.readList(SelectionAttribute::new);
        int tagsLength = in.readVInt();
        tags = new ArrayList<>(tagsLength);
        for (int i=0; i<tagsLength; i++) {
            tags.add(in.readString());
        }
    }

    @Override
    public CreateSandboxRequest read(StreamInput in) throws IOException {
        return new CreateSandboxRequest(in);
    }

    public static CreateSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser);
        return new CreateSandboxRequest(sandbox);
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
        Sandbox.writeToOutputStream(out, parentSandboxId, priority, resourceConsumptionLimits, selectionAttributes, tags);
    }
}
