/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is the POJO for Sandbox
 */
public class Sandbox implements ToXContentObject, Writeable {
    public static final String RESOURCE_CONSUMPTION_LIMITS = "resource_consumption_limits";
    public static final String TAGS = "tags";
    public static final String SELECTION_ATTRIBUTES = "selection_attributes";
    public static final String ID = "_id";
    public static final String PRIORITY = "priority";
    public static final String PARENT_ID = "parent_id";
    String _id;
    // Defines the priority for sandbox selection criteria in face of resource contention
    Integer priority;
    String parentId;
    List<SelectionAttribute> selectionAttributes;
    ResourceConsumptionLimits resourceConsumptionLimits;
    // This field will help giving meaningful names to sandbox object which later can be used to delete/retrieve objects
    List<String> tags;

    private static final String JVM_ALLOCATIONS = "jvm_allocations";
    private static final String CPU_USAGE = "cpu_usage";
    private static final String ATTRIBUTE_NAME = "attribute_name";
    private static final String ATTRIBUTE_REGEX_VALUE = "attribute_regex_value";

    private Sandbox(String _id, Integer priority, String parentId, List<SelectionAttribute> selectionAttributes,
                    ResourceConsumptionLimits resourceConsumptionLimits, List<String> tags) {
        Objects.requireNonNull(_id, "[_id] field should not be empty for sandbox");
        Objects.requireNonNull(priority, "[priority] field should not be empty for sandbox");
        Objects.requireNonNull(selectionAttributes, "[selectionAttributes] field should not be empty for sandbox");
        Objects.requireNonNull(resourceConsumptionLimits, "[resourceConsumptionLimits] field should not be empty for sandbox");

        this._id = _id;
        this.parentId = parentId;
        this.priority = priority;
        this.selectionAttributes = selectionAttributes;
        this.resourceConsumptionLimits = resourceConsumptionLimits;
        this.tags = tags;
    }

    public Sandbox(StreamInput in) throws IOException {
        _id = in.readString();
        parentId = in.readOptionalString();
        priority = in.readVInt();
        resourceConsumptionLimits = new ResourceConsumptionLimits(in);
        selectionAttributes = in.readList(SelectionAttribute::new);
        int numberOfTags = in.readVInt();
        tags = new ArrayList<>(numberOfTags);
        for (int i=0; i<numberOfTags; i++) {
            tags.add(in.readString());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(_id);
        writeToOutputStream(out, parentId, priority, resourceConsumptionLimits, selectionAttributes, tags);
    }

    public static void writeToOutputStream(StreamOutput out, String parentId, Integer priority, ResourceConsumptionLimits resourceConsumptionLimits, List<SelectionAttribute> selectionAttributes, List<String> tags) throws IOException {
        out.writeOptionalString(parentId);
        out.writeVInt(priority);
        resourceConsumptionLimits.writeTo(out);
        out.writeList(selectionAttributes);
        out.writeVInt(tags.size());
        for (String tag: tags) {
            out.writeString(tag);
        }
    }

    /**
     * Builder class for @link Sanddbox class
     */
    public static class Builder {
        String _id;
        Integer priority;
        String parentId;
        List<SelectionAttribute> selectionAttributes;
        ResourceConsumptionLimits resourceConsumptionLimits;
        List<String> tags;

        Builder() {
            tags = new ArrayList<>();
        }

        public static Sandbox fromXContent(XContentParser parser) throws IOException {

            while (parser.currentToken() != XContentParser.Token.START_OBJECT) {
                parser.nextToken();
            }

            if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
                throw new IllegalArgumentException("expected start object but got a " + parser.currentToken());
            }

            Builder builder = new Builder();
            XContentParser.Token token;
            String currentFieldName = "";
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else {
                    if (currentFieldName.equals(ID)) {
                        builder.id(parser.text());
                    } else if (currentFieldName.equals(PRIORITY)) {
                        builder.priority(parser.intValue());
                    } else if (currentFieldName.equals(PARENT_ID)) {
                        builder.parentId(parser.text());
                    } else if (currentFieldName.equals(SELECTION_ATTRIBUTES)) {

                        if (token != XContentParser.Token.START_ARRAY) {
                            throw new IllegalArgumentException(token + " should have been the start of an array token");
                        }
                        List<SelectionAttribute> selectionAttributes1 = new ArrayList<>();
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            SelectionAttribute selectionAttribute = SelectionAttribute.Builder.fromXContent(parser);
                            selectionAttributes1.add(selectionAttribute);
                        }
                        builder.selectionAttributes(selectionAttributes1);

                    } else if (currentFieldName.equals(RESOURCE_CONSUMPTION_LIMITS)) {
                        ResourceConsumptionLimits resourceConsumptionLimits1 = ResourceConsumptionLimits.Builder.fromXContent(parser);
                        builder.resourceConsumptionLimit(resourceConsumptionLimits1);
                    } else if (currentFieldName.equals(TAGS)) {
                        builder.tags(
                            parser.list()
                                .stream()
                                .map(x -> (String) x)
                                .collect(Collectors.toList())
                        );
                    } else {
                        throw new IllegalArgumentException(currentFieldName + " is not part of Sandbox object, malformed Sandbox info");
                    }
                }
            }
            return builder.build();
        }

        public Builder id(String _id) {
            this._id = _id;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder selectionAttributes(List<SelectionAttribute> selectionAttributes) {
            this.selectionAttributes = selectionAttributes;
            return this;
        }

        public Builder resourceConsumptionLimit(ResourceConsumptionLimits resourceConsumptionLimits) {
            this.resourceConsumptionLimits = resourceConsumptionLimits;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Sandbox build() {
            // generate new _id if needed
            if (_id == null) {
                _id = String.valueOf(Objects.hash(priority, selectionAttributes, resourceConsumptionLimits, tags));
            }
            return new Sandbox(_id, priority, parentId, selectionAttributes, resourceConsumptionLimits, tags);
        }


    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ID, _id);
        builder.field(PRIORITY, priority);
        if (parentId != null) {
            builder.field(PARENT_ID, parentId);
        }
        builder.startArray(SELECTION_ATTRIBUTES);
        for (SelectionAttribute selectionAttribute: selectionAttributes) {
            selectionAttribute.toXContent(builder, params);
        }
        builder.endArray();
        builder.field(RESOURCE_CONSUMPTION_LIMITS, resourceConsumptionLimits);
        builder.array(TAGS, tags);
        return builder;
    }

    /**
     * This defines the schema for the attributes which can be used to resolve sandboxes
     */
    public static class SelectionAttribute implements ToXContent, Writeable, Writeable.Reader<SelectionAttribute> {
        private final String attributeNane;
        private final String attributeValueRegex;

        public SelectionAttribute(StreamInput in) throws IOException {
            attributeNane = in.readString();
            attributeValueRegex = in.readString();
        }

        public SelectionAttribute(String attributeNane, String attributeValueRegex) {
            Objects.requireNonNull(attributeNane, "selection attribute name can't be null for sandbox");
            Objects.requireNonNull(attributeValueRegex, "selection value can't be null for sandbox");
            this.attributeNane = attributeNane;
            this.attributeValueRegex = attributeValueRegex;
        }

        /**
         * Builder for @link SelectionAttribute class
         */
        public static class Builder {
            String attributeName;
            String attributeRegexVal;
            public Builder() {}

            Builder attributeName(String attributeName) {
                this.attributeName = attributeName;
                return this;
            }

            Builder attributeRegexVal(String attributeRegexVal) {
                this.attributeRegexVal = attributeRegexVal;
                return this;
            }

            public static SelectionAttribute fromXContent(XContentParser parser) throws IOException {
                Builder builder = new Builder();
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (currentFieldName.equals(ATTRIBUTE_NAME)) {
                            builder.attributeName(parser.text());
                        } else if (currentFieldName.equals(ATTRIBUTE_REGEX_VALUE)) {
                            builder.attributeRegexVal(parser.text());
                        } else {
                            throw new IllegalArgumentException(currentFieldName + " is unrecognized field for Sandbox.SelectionAttribute");
                        }
                    }
                }
                return builder.build();
            }

            SelectionAttribute build() {
                return new SelectionAttribute(attributeName, attributeRegexVal);
            }
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(ATTRIBUTE_NAME, attributeNane);
            builder.field(ATTRIBUTE_REGEX_VALUE, attributeValueRegex);
            builder.endObject();
            return builder;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(attributeNane);
            out.writeString(attributeValueRegex);
        }

        @Override
        public SelectionAttribute read(StreamInput in) throws IOException {
            return new SelectionAttribute(in);
        }
    }

    /**
     * Schema for defining System resources per sandbox and the assigned limits
     */
    public static class ResourceConsumptionLimits implements ToXContent, Writeable {
        // Allocations in percent w.r.t. total heap
        private final Double jvmAllocations;
        // CPU usage in absolute terms irrespective of cores
        private final Double cpuUsage;

        public ResourceConsumptionLimits(Double jvmAllocations, Double cpuUsage) {
            this.jvmAllocations = jvmAllocations;
            this.cpuUsage = cpuUsage;
        }

        public ResourceConsumptionLimits(StreamInput in) throws IOException {
            jvmAllocations = in.readDouble();
            cpuUsage = in.readDouble();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(JVM_ALLOCATIONS, jvmAllocations);
            builder.field(CPU_USAGE, cpuUsage);
            builder.endObject();
            return builder;
        }


        static class Builder {
            private Double jvmAllocationsLimit;
            private Double cpuUsageLimit;

            public Builder() {}

            Builder jvmAllocations(double jvmAllocationsLimit) {
                this.jvmAllocationsLimit = jvmAllocationsLimit;
                return this;
            }

            Builder cpuUsage(double cpuUsageLimit) {
                this.cpuUsageLimit = cpuUsageLimit;
                return this;
            }

            public static ResourceConsumptionLimits fromXContent(XContentParser parser) throws IOException {
                Builder builder = new Builder();
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (currentFieldName.equals(JVM_ALLOCATIONS)) {
                            builder.jvmAllocations(parser.doubleValue());
                        } else if (currentFieldName.equals(CPU_USAGE)) {
                            builder.cpuUsage(parser.doubleValue());
                        } else {
                            throw new IllegalArgumentException(currentFieldName + " is unrecognized" +
                                " field for Sandbox.ResourceConsumptionLimits");
                        }
                    }
                }
                return builder.build();
            }
            ResourceConsumptionLimits build() {
                return new ResourceConsumptionLimits(jvmAllocationsLimit, cpuUsageLimit);
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeDouble(jvmAllocations);
            out.writeDouble(cpuUsage);
        }
    }

    public String get_id() {
        return _id;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getParentId() {
        return parentId;
    }

    public List<SelectionAttribute> getSelectionAttributes() {
        return selectionAttributes;
    }

    public ResourceConsumptionLimits getResourceConsumptionLimits() {
        return resourceConsumptionLimits;
    }

    public List<String> getTags() {
        return tags;
    }
}
