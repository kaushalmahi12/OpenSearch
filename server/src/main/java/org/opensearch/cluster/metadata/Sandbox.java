/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.cluster.metadata;

import org.opensearch.cluster.AbstractDiffable;
import org.opensearch.cluster.Diff;
import org.opensearch.common.annotation.ExperimentalApi;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.search.sandboxing.SandboxResourceType;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Class to define the Sandbox schema
 * {
 *     "name": "analytics",
 *     "resourceLimits": [
 *          {
 *              "resourceName": "jvm",
 *              "value": 0.4
 *          }
 *     ]
 * }
 */
@ExperimentalApi
public class Sandbox extends AbstractDiffable<Sandbox> implements ToXContentObject {

    public static final int MAX_CHARS_ALLOWED_IN_NAME = 50;
    private final String name;
    private final List<ResourceLimit> resourceLimits;
    private final SandboxMode mode;

    private static final EnumSet<SandboxResourceType> ALLOWED_RESOURCES = EnumSet.of(SandboxResourceType.JVM);

    public static final ParseField NAME_FIELD = new ParseField("name");
    public static final ParseField RESOURCE_LIMITS_FIELD = new ParseField("resourceLimits");
    public static final ParseField MODE_FIELD = new ParseField("mode");

    private static final int numberOfAvailableProcessors = Runtime.getRuntime().availableProcessors();
    private static final long totalAvailableJvmMemory = Runtime.getRuntime().totalMemory();

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<Sandbox, Void> PARSER = new ConstructingObjectParser<>(
        "SandboxParser",
        args -> new Sandbox((String) args[0], (List<ResourceLimit>) args[1], (String) args[2])
    );

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), NAME_FIELD);
        PARSER.declareObjectArray(
            ConstructingObjectParser.constructorArg(),
            (p, c) -> ResourceLimit.fromXContent(p),
            RESOURCE_LIMITS_FIELD
        );
        PARSER.declareString(ConstructingObjectParser.constructorArg(), MODE_FIELD);
    }

    public Sandbox(final String name, final List<ResourceLimit> resourceLimits, final String modeName) {
        Objects.requireNonNull(name, "Sandbox.name can't be null");
        Objects.requireNonNull(resourceLimits, "Sandbox.resourceLimits can't be null");

        if (name.length() > MAX_CHARS_ALLOWED_IN_NAME) {
            throw new IllegalArgumentException("Sandbox.name shouldn't be more than 50 chars long");
        }

        if (resourceLimits.isEmpty()) {
            throw new IllegalArgumentException("Sandbox.resourceLimits should at least have 1 resource limit");
        }

        this.name = name;
        this.resourceLimits = resourceLimits;
        this.mode = SandboxMode.fromName(modeName);
    }

    public Sandbox(StreamInput in) throws IOException {
        this(in.readString(), in.readList(ResourceLimit::new), in.readString());
    }

    /**
     * Class to hold the system resource limits;
     * sample Schema
     */
    @ExperimentalApi
    public static class ResourceLimit implements Writeable, ToXContentObject {
        private final SandboxResourceType resourceType;
        private final Double threshold;
        private final Long thresholdInLong;

        static final ParseField RESOURCE_TYPE_FIELD = new ParseField("resourceType");
        static final ParseField THRESHOLD_FIELD = new ParseField("threshold");
        static final ParseField THRESHOLD_IN_LONG_FIELD = new ParseField("thresholdInLong");

        public static final ConstructingObjectParser<ResourceLimit, Void> PARSER = new ConstructingObjectParser<>(
            "ResourceLimitParser",
            args -> new ResourceLimit((String) args[0], (Double) args[1])
        );

        static {
            PARSER.declareString(ConstructingObjectParser.constructorArg(), RESOURCE_TYPE_FIELD);
            PARSER.declareDouble(ConstructingObjectParser.constructorArg(), THRESHOLD_FIELD);
            PARSER.declareDouble(ConstructingObjectParser.constructorArg(), THRESHOLD_IN_LONG_FIELD);
        }

        public ResourceLimit(String resourceType, Double threshold) {
            Objects.requireNonNull(resourceType, "resourceName can't be null");
            Objects.requireNonNull(threshold, "resource value can't be null");

            if (Double.compare(threshold, 1.0) > 0) {
                throw new IllegalArgumentException("resource value should be less than 1.0");
            }

            if (!ALLOWED_RESOURCES.contains(SandboxResourceType.fromString(resourceType.toUpperCase(Locale.ROOT)))) {
                throw new IllegalArgumentException("Invalid resource, valid resources : " + ALLOWED_RESOURCES);
            }
            this.resourceType = SandboxResourceType.fromString(resourceType);
            this.threshold = threshold;
            this.thresholdInLong = calculateThresholdInLong(threshold);
        }

        public ResourceLimit(Sandbox resourceType, Double threshold) {
            new ResourceLimit(resourceType.toString(), threshold);
        }

        private Long calculateThresholdInLong(Double threshold) {
            if (this.resourceType == SandboxResourceType.JVM) {
                return (long) (threshold * totalAvailableJvmMemory);
            }
            return 0L;
        }

        public ResourceLimit(StreamInput in) throws IOException {
            this(in.readString(), in.readDouble());
        }

        /**
         * Write this into the {@linkplain StreamOutput}.
         *
         * @param out
         */
        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(resourceType.toString());
            out.writeDouble(threshold);
        }

        /**
         * @param builder
         * @param params
         * @return
         * @throws IOException
         */
        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(RESOURCE_TYPE_FIELD.getPreferredName(), resourceType);
            builder.field(THRESHOLD_FIELD.getPreferredName(), threshold);
            builder.endObject();
            return builder;
        }

        public static ResourceLimit fromXContent(final XContentParser parser) throws IOException {
            return PARSER.parse(parser, null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceLimit that = (ResourceLimit) o;
            return Objects.equals(resourceType, that.resourceType) && Objects.equals(threshold, that.threshold);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceType, threshold);
        }

        public SandboxResourceType getResourceType() {
            return resourceType;
        }

        public Double getThreshold() {
            return threshold;
        }

        public Long getThresholdInLong() {
            return thresholdInLong;
        }
    }

    /**
     * This enum models the different sandbox modes
     */
    @ExperimentalApi
    public enum SandboxMode {
        SOFT("soft"),
        ENFORCED("enforced"),
        MONITOR("monitor");

        private final String name;

        SandboxMode(String mode) {
            this.name = mode;
        }

        public String getName() {
            return name;
        }

        public static SandboxMode fromName(String s) {
            switch (s) {
                case "soft":
                    return SOFT;
                case "enforced":
                    return ENFORCED;
                case "monitor":
                    return MONITOR;
                default:
                    throw new IllegalArgumentException("Invalid value for SandboxMode: " + s);
            }
        }

    }

    /**
     * Write this into the {@linkplain StreamOutput}.
     *
     * @param out
     */
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeList(resourceLimits);
        out.writeString(mode.getName());
    }

    /**
     * @param builder
     * @param params
     * @return
     * @throws IOException
     */
    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        builder.startObject();
        builder.field(NAME_FIELD.getPreferredName(), name);
        builder.field(RESOURCE_LIMITS_FIELD.getPreferredName(), resourceLimits);
        builder.field(MODE_FIELD.getPreferredName(), mode.getName());
        builder.endObject();
        return builder;
    }

    public static Sandbox fromXContent(final XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    public static Diff<Sandbox> readDiff(final StreamInput in) throws IOException {
        return readDiffFrom(Sandbox::new, in);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sandbox that = (Sandbox) o;
        return Objects.equals(name, that.name) && Objects.equals(resourceLimits, that.resourceLimits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resourceLimits);
    }

    public String getName() {
        return name;
    }

    public SandboxMode getMode() {
        return mode;
    }

    public List<ResourceLimit> getResourceLimits() {
        return resourceLimits;
    }

    public ResourceLimit getResourceLimitFor(SandboxResourceType resourceType) {
        return resourceLimits.stream()
            .filter(resourceLimit -> resourceLimit.getResourceType().equals(resourceType))
            .findFirst()
            .orElseGet(() -> new ResourceLimit(resourceType.toString(), 100.0));
    }
}
