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

    public static final String CPU = "cpu";
    public static final String JVM = "jvm";
    private static final String ATTRIBUTE_NAME = "attribute_name";
    private static final String ATTRIBUTE_VALUE_PREFIX = "attribute_value_prefix";

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

    private Sandbox(String _id, Integer priority, String parentId, List<SelectionAttribute> selectionAttributes,
                    ResourceConsumptionLimits resourceConsumptionLimits, List<String> tags, boolean updateRequest) {
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

        public static Sandbox fromXContent(XContentParser parser, boolean updateRequest) throws IOException {

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
            return builder.build(updateRequest);
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

        public Sandbox build(boolean updateRequest) {
            // generate new _id if needed (in create api call)
            if (!updateRequest) {
                _id = String.valueOf(Objects.hash(priority, selectionAttributes, resourceConsumptionLimits, tags));
                return new Sandbox(_id, priority, parentId, selectionAttributes, resourceConsumptionLimits, tags);
            } else {
                return new Sandbox(_id, priority, parentId, selectionAttributes, resourceConsumptionLimits, tags, true);
            }
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
        resourceConsumptionLimits.toXContent(builder, params);
        builder.array(TAGS, tags);
        builder.endObject();
        return builder;
    }

    /**
     * This defines the schema for the attributes which can be used to resolve sandboxes
     */
    public static class SelectionAttribute implements ToXContent, Writeable, Writeable.Reader<SelectionAttribute> {
        private final String attributeNane;
        private final String attributeValuePrefix;

        public SelectionAttribute(StreamInput in) throws IOException {
            attributeNane = in.readString();
            attributeValuePrefix = in.readString();
        }

        public SelectionAttribute(String attributeNane, String attributeValuePrefix) {
            Objects.requireNonNull(attributeNane, "selection attribute name can't be null for sandbox");
            Objects.requireNonNull(attributeValuePrefix, "selection value can't be null for sandbox");

            if (attributeValuePrefix.isEmpty()) {
                throw new IllegalArgumentException("sandbox selection attribute value length should be greater than 1");
            }
            this.attributeNane = attributeNane;
            this.attributeValuePrefix = attributeValuePrefix;
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
                            builder = builder.attributeName(parser.text());
                        } else if (currentFieldName.equals(ATTRIBUTE_VALUE_PREFIX)) {
                            builder = builder.attributeRegexVal(parser.text());
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
            builder.field(ATTRIBUTE_VALUE_PREFIX, attributeValuePrefix);
            builder.endObject();
            return builder;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(attributeNane);
            out.writeString(attributeValuePrefix);
        }

        @Override
        public SelectionAttribute read(StreamInput in) throws IOException {
            return new SelectionAttribute(in);
        }

        public boolean overshadows(final SelectionAttribute other) {
            return attributeNane.equals(other.attributeNane) && attributeValuePrefix.startsWith(other.attributeValuePrefix);
        }

        public String getAttributeNane() {
            return attributeNane;
        }

        public String getAttributeValuePrefix() {
            return attributeValuePrefix;
        }
    }

    /**
     * Class to define system resource level thresholds
     */
    public static class SystemResource implements ToXContent, Writeable {
        static final String LOW = "low";
        static final String HIGH = "high";
        double low;
        double high;
        String name;

        SystemResource(double low, double high, String name) {
            isValid(low, high);
            this.low = low;
            this.high = high;
            this.name = name;
        }

        private static void isValid(double low, double high) {
            if (low > high) {
                throw new IllegalArgumentException("System resource low limit can't be greater than high.");
            }
        }

        public SystemResource(StreamInput in) throws  IOException {
            this.low = in.readDouble();
            this.high = in.readDouble();
            this.name = in.readString();
            isValid(low, high);
        }


        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeDouble(low);
            out.writeDouble(high);
            out.writeString(name);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(name);
            builder.field(LOW, low);
            builder.field(HIGH, high);
            builder.endObject();
            return builder;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder class for sandbox
         * @opensearch.internal
         */
        public static class Builder {
            String name;
            double low;
            double high;

            Builder() {
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder low(double low) { this.low = low; return this; }

            public Builder high(double high) { this.high = high; return this; }


            public static SystemResource fromXContent(XContentParser parser, String name) throws IOException {
                Builder builder = new Builder();
                builder.name(name);
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (currentFieldName.equals(LOW)) {
                            builder = builder.low(parser.doubleValue());
                        } else if (currentFieldName.equals(HIGH)) {
                            builder = builder.high(parser.doubleValue());
                        } else {
                            throw new IllegalArgumentException(currentFieldName + " is an unexpected field name for SystemResource");
                        }
                    }
                }
                return builder.build();
            }

            public SystemResource build() {
                return new SystemResource(low, high, name);
            }
        }

        public double getLow() {
            return low;
        }

        public double getHigh() {
            return high;
        }
    }

    /**
     * Schema for defining System resources per sandbox and the assigned limits
     */
    public static class ResourceConsumptionLimits implements ToXContent, Writeable {
        // Allocations in percent w.r.t. total heap
        private final SystemResource jvm;
        // CPU usage in absolute terms irrespective of cores
        private final SystemResource cpu;

        public ResourceConsumptionLimits(SystemResource jvmAllocations, SystemResource cpuUsage) {
            this.jvm = jvmAllocations;
            this.cpu = cpuUsage;
        }

        public ResourceConsumptionLimits(StreamInput in) throws IOException {
            jvm = new SystemResource(in);
            cpu = new SystemResource(in);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(RESOURCE_CONSUMPTION_LIMITS);
            jvm.toXContent(builder, params);
            cpu.toXContent(builder, params);
            builder.endObject();
            return builder;
        }


        static class Builder {
            private SystemResource jvm;
            private SystemResource cpu;

            public Builder() {}

            Builder jvmAllocations(SystemResource jvmAllocationsLimit) {
                this.jvm = jvmAllocationsLimit;
                return this;
            }

            Builder cpuUsage(SystemResource cpuUsageLimit) {
                this.cpu = cpuUsageLimit;
                return this;
            }

            public static ResourceConsumptionLimits fromXContent(XContentParser parser) throws IOException {
                Builder builder = new Builder();
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token == XContentParser.Token.START_OBJECT) {
                       SystemResource systemResource = SystemResource.Builder.fromXContent(parser, currentFieldName);
                       if (systemResource.getName().equals(CPU)) {
                           builder = builder.cpuUsage(systemResource);
                       } else if (systemResource.getName().equals(JVM)) {
                           builder = builder.jvmAllocations(systemResource);
                       } else {
                           throw new IllegalArgumentException("unexpected system resource for");
                       }
                    }
                }
                return builder.build();
            }
            ResourceConsumptionLimits build() {
                return new ResourceConsumptionLimits(jvm, cpu);
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            jvm.writeTo(out);
            cpu.writeTo(out);
        }

        public SystemResource getJvm() {
            return jvm;
        }

        public SystemResource getCpu() {
            return cpu;
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

    public boolean hasOvershadowingSelectionAttribute(final Sandbox sandbox) {
        boolean overshadows = true;
        for (SelectionAttribute selectionAttribute: selectionAttributes) {
            for (SelectionAttribute otherSelectionAttribute: sandbox.getSelectionAttributes()) {
                if (!selectionAttribute.overshadows(otherSelectionAttribute)) {
                    overshadows = false;
                    break;
                }
            }
            if (overshadows) break;
        }
        return overshadows;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
