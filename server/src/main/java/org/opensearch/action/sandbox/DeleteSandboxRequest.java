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
//    String parentSandboxId;
//    Integer priority;
//    ResourceConsumptionLimits resourceConsumptionLimits;
//    List<SelectionAttribute> selectionAttributes;
//    List<String> tags;
    public DeleteSandboxRequest() {
//        tags = new ArrayList<>();
    }

    public DeleteSandboxRequest(String _id) {
        this._id = _id;
    }

    public DeleteSandboxRequest(Sandbox sandbox) {
        this._id = sandbox.get_id();
    }

    public DeleteSandboxRequest(StreamInput in) throws IOException {
        super(in);
        this._id = in.readString();
//        parentSandboxId = in.readOptionalString();
//        priority = in.readVInt();
//        resourceConsumptionLimits = new ResourceConsumptionLimits(in);
//        selectionAttributes = in.readList(SelectionAttribute::new);
//        int tagsLength = in.readVInt();
//        tags = new ArrayList<>(tagsLength);
//        for (int i=0; i<tagsLength; i++) {
//            tags.add(in.readString());
//        }
    }

    @Override
    public DeleteSandboxRequest read(StreamInput in) throws IOException {
        return new DeleteSandboxRequest(in);
    }

    public static DeleteSandboxRequest fromXContent(XContentParser parser) throws IOException {
        Sandbox sandbox = Sandbox.Builder.fromXContent(parser);
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
//
//    public String getParentSandboxId() {
//        return parentSandboxId;
//    }
//
//    public Integer getPriority() {
//        return priority;
//    }
//
//    public ResourceConsumptionLimits getResourceConsumptionLimits() {
//        return resourceConsumptionLimits;
//    }
//
//    public List<SelectionAttribute> getSelectionAttributes() {
//        return selectionAttributes;
//    }
//
//    public List<String> getTags() {
//        return tags;
//    }
//
//    public void setParentSandboxId(String parentSandboxId) {
//        this.parentSandboxId = parentSandboxId;
//    }
//
//    public void setPriority(Integer priority) {
//        this.priority = priority;
//    }
//
//    public void setResourceConsumptionLimits(ResourceConsumptionLimits resourceConsumptionLimits) {
//        this.resourceConsumptionLimits = resourceConsumptionLimits;
//    }
//
//    public void setSelectionAttributes(List<SelectionAttribute> selectionAttributes) {
//        this.selectionAttributes = selectionAttributes;
//    }
//
//    public void setTags(List<String> tags) {
//        this.tags = tags;
//    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
       // Sandbox.writeToOutputStream(out, parentSandboxId, priority, resourceConsumptionLimits, selectionAttributes, tags);
    }
}
