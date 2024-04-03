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
    String _id;
    String name;
    SandboxAttributes sandboxAttributes;
    ResourceConsumptionLimits resourceConsumptionLimits;
    String enforcement;

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String RESOURCES = "resources";
    public static final String ATTRIBUTES = "attributes";
    public static final String JVM = "jvm";
    public static final String ENFORCEMENT = "enforcement";

    private Sandbox(String _id, String name, SandboxAttributes sandboxAttributes, ResourceConsumptionLimits resourceConsumptionLimits, String enforcement, boolean optionalSandboxFields) {
        if (!optionalSandboxFields) {
            Objects.requireNonNull(_id, "[_id] field should not be empty for sandbox");
            Objects.requireNonNull(name, "[name] field should not be empty for sandbox");
            Objects.requireNonNull(sandboxAttributes, "[selectionAttributes] field should not be empty for sandbox");
            Objects.requireNonNull(resourceConsumptionLimits, "[resourceConsumptionLimits] field should not be empty for sandbox");
            Objects.requireNonNull(enforcement, "[enforcement] field should not be empty for sandbox");

        }

        isValidSandbox(name, enforcement);
        this._id = _id;
        this.name = name;
        this.sandboxAttributes = sandboxAttributes;
        this.resourceConsumptionLimits = resourceConsumptionLimits;
        this.enforcement = enforcement;
    }

//    private Sandbox(String _id, String name, SandboxAttributes sandboxAttributes, ResourceConsumptionLimits resourceConsumptionLimits, String enforcement, boolean updateRequest) {
//        this._id = _id;
//        this.name = name;
//        this.sandboxAttributes = sandboxAttributes;
//        this.resourceConsumptionLimits = resourceConsumptionLimits;
//        this.enforcement = enforcement;
//    }

    public Sandbox(StreamInput in) throws IOException {
        _id = in.readString();
        name = in.readString();
        resourceConsumptionLimits = new ResourceConsumptionLimits(in);
        sandboxAttributes = new SandboxAttributes(in);
        enforcement = in.readString();
        isValidSandbox(name, enforcement);
    }

    private void isValidSandbox(String name, String enforcement) {
        if (name != null) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Sandbox name cannot be empty");
            }
            if (name.startsWith("-")||name.startsWith("_")) {
                throw new IllegalArgumentException("Sandbox name cannot start with '_' or '-'.");
            }
            if (name.matches(".*[ ,:\"*+/\\\\|?#><].*")) {
                throw new IllegalArgumentException("Sandbox names can't contain spaces, commas, quotes, slashes, :, *, +, |, ?, #, >, or <");
            }
        }
        if (enforcement != null) {
            if (!enforcement.equals("monitor")) { // we only allow monitor right now
                throw new IllegalArgumentException("We only support monitor enforcement mode right now");
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(_id);
        writeToOutputStream(out, name, resourceConsumptionLimits, sandboxAttributes, enforcement);
    }

    public static void writeToOutputStream(StreamOutput out,String name, ResourceConsumptionLimits resourceConsumptionLimits, SandboxAttributes sandboxAttributes, String enforcement) throws IOException {
        out.writeString(name);
        resourceConsumptionLimits.writeTo(out);
        sandboxAttributes.writeTo(out);
        out.writeString(enforcement);
    }

    /**
     * Builder class for @link Sandbox class
     */
    public static class Builder {
        String _id;
        String name;
        SandboxAttributes sandboxAttributes;
        ResourceConsumptionLimits resourceConsumptionLimits;
        String enforcement;

        Builder() {}

        public static Sandbox fromXContent(XContentParser parser, boolean optionalSandboxFields) throws IOException {

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
                    } else if (currentFieldName.equals(NAME)) {
                        builder.name(parser.text());
                    } else if (currentFieldName.equals(ENFORCEMENT)) {
                        builder.enforcement(parser.text());
                    } else if (currentFieldName.equals(ATTRIBUTES)) {
                        SandboxAttributes sandboxAttributes1 = SandboxAttributes.Builder.fromXContent(parser);
                        builder.sandboxAttributes(sandboxAttributes1);
                    } else if (currentFieldName.equals(RESOURCES)) {
                        ResourceConsumptionLimits resourceConsumptionLimits1 = ResourceConsumptionLimits.Builder.fromXContent(parser);
                        builder.resourceConsumptionLimit(resourceConsumptionLimits1);
                    } else {
                        throw new IllegalArgumentException(currentFieldName + " is not part of Sandbox object, malformed Sandbox info");
                    }
                }
            }
            return builder.build(optionalSandboxFields);
        }

        public Builder id(String _id) {
            this._id = _id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sandboxAttributes(SandboxAttributes sandboxAttributes) {
            this.sandboxAttributes = sandboxAttributes;
            return this;
        }

        public Builder resourceConsumptionLimit(ResourceConsumptionLimits resourceConsumptionLimits) {
            this.resourceConsumptionLimits = resourceConsumptionLimits;
            return this;
        }

        public Builder enforcement(String enforcement) {
            this.enforcement = enforcement;
            return this;
        }

        public Sandbox build(boolean optionalSandboxFields) {
            if (!optionalSandboxFields) {
                _id = String.valueOf(Objects.hash(name, sandboxAttributes, resourceConsumptionLimits, enforcement));
                return new Sandbox(_id, name, sandboxAttributes, resourceConsumptionLimits, enforcement, false);
            } else {
                return new Sandbox(_id, name, sandboxAttributes, resourceConsumptionLimits, enforcement, true);
            }
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(NAME, name);
        sandboxAttributes.toXContent(builder, params);
        resourceConsumptionLimits.toXContent(builder, params);
        builder.field(ENFORCEMENT, enforcement);
        builder.endObject();
        return builder;
    }

    /**
     * This defines the schema for the attributes which can be used to resolve sandboxes
     */
    public static class SandboxAttributes implements ToXContent, Writeable, Writeable.Reader<SandboxAttributes> {
        private static final String INDICES_NAME = "indices_name";
        private final String indicesValues;

        public SandboxAttributes(StreamInput in) throws IOException {
            indicesValues = in.readString();
            isValidAttribute(indicesValues);
        }

        public SandboxAttributes(String indicesValues) {
            Objects.requireNonNull(indicesValues, "Indices value can't be null for sandbox");
            if (indicesValues.isEmpty()) {
                throw new IllegalArgumentException("Indices value length should be greater than 0");
            }
            isValidAttribute(indicesValues);
            this.indicesValues = indicesValues;
        }

        private void isValidAttribute(String indicesValues) {
            if (indicesValues.startsWith("*")) {
                throw new IllegalArgumentException("Attribute value cannot start with patterns");
            }
            if (indicesValues.matches(".*[:, \"+/\\\\|#?<>].*")) {
                throw new IllegalArgumentException("Attribute value can't contain spaces, commas, quotes, slashes, :, +, |, ?, #, >, or <");
            }
        }

        public String getIndicesValues() {
            return indicesValues;
        }

        /**
         * Builder for @link SelectionAttribute class
         */
        public static class Builder {
            String indicesValues;
            public Builder() {}

            Builder indicesValues(String indicesValues) {
                this.indicesValues = indicesValues;
                return this;
            }

            public static SandboxAttributes fromXContent(XContentParser parser) throws IOException {
                Builder builder = new Builder();
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (currentFieldName.equals(INDICES_NAME)) {
                            builder = builder.indicesValues(parser.text());
                        } else {
                            throw new IllegalArgumentException(currentFieldName + " is unrecognized field for Sandbox.SelectionAttribute");
                        }
                    }
                }
                return builder.build();
            }

            SandboxAttributes build() {
                return new SandboxAttributes(indicesValues);
            }
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(ATTRIBUTES);
            builder.field(INDICES_NAME, indicesValues);
            builder.endObject();
            return builder;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(indicesValues);
        }

        @Override
        public SandboxAttributes read(StreamInput in) throws IOException {
            return new SandboxAttributes(in);
        }
    }

    /**
     * Class to define system resource level thresholds
     */
    public static class SystemResource implements ToXContent, Writeable {
        static final String ALLOCATION = "allocation";
        double allocation;
        String name;

        SystemResource(double allocation, String name) {
            isValid(allocation, name);
            this.allocation = allocation;
            this.name = name;
        }

        public SystemResource(StreamInput in) throws  IOException {
            this.allocation = in.readDouble();
            this.name = in.readString();
            isValid(allocation, name);
        }

        private static void isValid(double allocation, String name) {
            if (allocation < 0 || allocation > 1) {
                throw new IllegalArgumentException("System resource allocation should be between 0 and 1.");
            }
            String str = String.valueOf(allocation);
            if (str.contains(".") && str.split("\\.")[1].length() > 2) {
                throw new IllegalArgumentException("System resource allocation should have at most two digits after the decimal point");
            }
            if (!name.equals(JVM)) {
                throw new IllegalArgumentException("We only support resource name to be jvm at the moment");
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeDouble(allocation);
            out.writeString(name);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(name);
            builder.field(ALLOCATION, allocation);
            builder.endObject();
            return builder;
        }

        public String getName() {
            return name;
        }

        public double getAllocation() {
            return allocation;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Class to build system resource level thresholds
         */
        public static class Builder {
            String name;
            double allocation;

            Builder() {
            }

            Builder name(String name) {
                this.name = name;
                return this;
            }

            Builder allocation(double allocation) { this.allocation = allocation; return this; }

            public static SystemResource fromXContent(XContentParser parser, String name) throws IOException {
                Builder builder = new Builder();
                builder.name(name);
                XContentParser.Token token;
                String currentFieldName = "";
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if (currentFieldName.equals(ALLOCATION)) {
                            builder = builder.allocation(parser.doubleValue());
                        } else {
                            throw new IllegalArgumentException(currentFieldName + " is an unexpected field name for SystemResource");
                        }
                    }
                }
                return builder.build();
            }

            public SystemResource build() {
                return new SystemResource(allocation, name);
            }
        }
    }

    /**
     * Schema for defining System resources per sandbox and the assigned limits
     */
    public static class ResourceConsumptionLimits implements ToXContent, Writeable {
        // Allocations in percent w.r.t. total heap
        private final SystemResource jvm;

        public ResourceConsumptionLimits(SystemResource jvmAllocations) {
            this.jvm = jvmAllocations;
        }

        public ResourceConsumptionLimits(StreamInput in) throws IOException {
            jvm = new SystemResource(in);
        }

        public SystemResource getJvm() {
            return jvm;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(RESOURCES);
            jvm.toXContent(builder, params);
            builder.endObject();
            return builder;
        }

        static class Builder {
            private SystemResource jvm;

            public Builder() {}

            Builder jvmAllocations(SystemResource jvmAllocationsLimit) {
                this.jvm = jvmAllocationsLimit;
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
                        if (systemResource.getName().equals(JVM)) {
                            builder = builder.jvmAllocations(systemResource);
                        } else {
                            throw new IllegalArgumentException("unexpected system resource");
                        }
                    }
                }
                return builder.build();
            }
            ResourceConsumptionLimits build() {
                return new ResourceConsumptionLimits(jvm);
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            jvm.writeTo(out);
        }
    }

    public String get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public SandboxAttributes getSandboxAttributes() {
        return sandboxAttributes;
    }

    public ResourceConsumptionLimits getResourceConsumptionLimits() {
        return resourceConsumptionLimits;
    }

    public String getEnforcement() {
        return enforcement;
    }
}
