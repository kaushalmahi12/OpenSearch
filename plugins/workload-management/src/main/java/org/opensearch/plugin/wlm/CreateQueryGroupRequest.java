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
import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.cluster.metadata.QueryGroup.ResiliencyMode;

import org.opensearch.common.UUIDs;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.XContentParser;
import org.joda.time.Instant;
import org.opensearch.search.ResourceType;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Objects;

import static org.opensearch.cluster.metadata.QueryGroup.MAX_CHARS_ALLOWED_IN_NAME;
import static org.opensearch.search.ResourceType.isValidResourceType;

/**
 * A request for create QueryGroup
 * User input schema:
 *  {
 *    "jvm": 0.4,
 *    "mode": "enforced",
 *    "name": "analytics"
 *  }
 *
 * @opensearch.internal
 */
public class CreateQueryGroupRequest extends ActionRequest implements Writeable.Reader<CreateQueryGroupRequest> {
    private String name;
    private String _id;
    private ResiliencyMode resiliencyMode;
    private Map<String, Object> resourceLimits;
    private long updatedAtInMillis;

    /**
     * Default constructor for CreateQueryGroupRequest
     */
    public CreateQueryGroupRequest() {}

    /**
     * Constructor for CreateQueryGroupRequest
     * @param queryGroup - A {@link QueryGroup} object
     */
    public CreateQueryGroupRequest(QueryGroup queryGroup) {
        this.name = queryGroup.getName();
        this._id = queryGroup.get_id();
        Map<ResourceType, Object> resourceTypesMap = queryGroup.getResourceLimits();
        Map<String, Object> resourceLimits_ = new HashMap<>();
        for (Map.Entry<ResourceType, Object> resource : resourceTypesMap.entrySet()) {
            resourceLimits_.put(resource.getKey().getName(), resource.getValue());
        }
        this.resourceLimits = resourceLimits_;
        this.resiliencyMode = queryGroup.getResiliencyMode();
        this.updatedAtInMillis = queryGroup.getUpdatedAtInMillis();
        verifyCreateQueryGroupRequest(name, _id, resiliencyMode, resourceLimits, updatedAtInMillis);
    }

    /**
     * Constructor for CreateQueryGroupRequest
     * @param name - QueryGroup name for CreateQueryGroupRequest
     * @param _id - QueryGroup _id for CreateQueryGroupRequest
     * @param mode - QueryGroup mode for CreateQueryGroupRequest
     * @param resourceLimits - QueryGroup resourceLimits for CreateQueryGroupRequest
     * @param updatedAtInMillis - QueryGroup updated time in millis for CreateQueryGroupRequest
     */
    public CreateQueryGroupRequest(
        String name,
        String _id,
        ResiliencyMode mode,
        Map<String, Object> resourceLimits,
        long updatedAtInMillis
    ) {
        this.name = name;
        this._id = _id;
        this.resourceLimits = resourceLimits;
        this.resiliencyMode = mode;
        this.updatedAtInMillis = updatedAtInMillis;
        verifyCreateQueryGroupRequest(name, _id, mode, resourceLimits, updatedAtInMillis);
    }

    /**
     * Constructor for CreateQueryGroupRequest
     * @param in - A {@link StreamInput} object
     */
    public CreateQueryGroupRequest(StreamInput in) throws IOException {
        super(in);
        name = in.readString();
        _id = in.readString();
        resiliencyMode = ResiliencyMode.fromName(in.readString());
        resourceLimits = in.readMap();
        updatedAtInMillis = in.readLong();
        verifyCreateQueryGroupRequest(name, _id, resiliencyMode, resourceLimits, updatedAtInMillis);
    }

    /**
     * Verification for CreateQueryGroupRequest
     * @param name - QueryGroup name for CreateQueryGroupRequest
     * @param _id - QueryGroup _id for CreateQueryGroupRequest
     * @param mode - QueryGroup mode for CreateQueryGroupRequest
     * @param resourceLimits - QueryGroup resourceLimits for CreateQueryGroupRequest
     * @param updatedAtInMillis - QueryGroup updated time in millis for CreateQueryGroupRequest
     */
    private void verifyCreateQueryGroupRequest(
        String name,
        String _id,
        ResiliencyMode mode,
        Map<String, Object> resourceLimits,
        long updatedAtInMillis
    ) {
        Objects.requireNonNull(_id, "QueryGroup._id can't be null");
        Objects.requireNonNull(name, "QueryGroup.name can't be null");
        Objects.requireNonNull(resourceLimits, "QueryGroup.resourceLimits can't be null");
        Objects.requireNonNull(mode, "QueryGroup.mode can't be null");
        Objects.requireNonNull(updatedAtInMillis, "QueryGroup.updatedAtInMillis can't be null");

        validateName(name);
        validateMode(mode);
        validateResourceLimits(resourceLimits);
        validateUpdatedAtInMillis(updatedAtInMillis);
    }

    /**
     * Verification for CreateQueryGroupRequest.name
     * @param name - name to be verified
     */
    public static void validateName(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("QueryGroup.name cannot be empty");
        }
        if (name.length() > MAX_CHARS_ALLOWED_IN_NAME) {
            throw new IllegalArgumentException("QueryGroup.name shouldn't be more than 50 chars long");
        }
        if (name.startsWith("-") || name.startsWith("_")) {
            throw new IllegalArgumentException("QueryGroup.name cannot start with '_' or '-'.");
        }
        if (name.matches(".*[ ,:\"*+/\\\\|?#><].*")) {
            throw new IllegalArgumentException("QueryGroup.name can't contain spaces, commas, quotes, slashes, :, *, +, |, ?, #, >, or <");
        }
    }

    /**
     * Verification for CreateQueryGroupRequest.mode
     * @param mode - mode to be verified
     */
    public static void validateMode(ResiliencyMode mode) {
        if (!mode.getName().equals("monitor")) {
            throw new IllegalArgumentException("QueryGroup.mode must be monitor");
        }
    }

    /**
     * Verification for CreateQueryGroupRequest.resourceLimits
     * @param resourceLimits - resourceLimits to be verified
     */
    public static void validateResourceLimits(Map<String, Object> resourceLimits) {
        if (resourceLimits.isEmpty()) {
            throw new IllegalArgumentException("QueryGroup.resourceLimits should at least have 1 resource limit.");
        }
        for (Map.Entry<String, Object> resource : resourceLimits.entrySet()) {
            String resourceName = resource.getKey();
            Double threshold = (Double) resource.getValue();
            Objects.requireNonNull(resourceName, "resourceName can't be null");
            Objects.requireNonNull(threshold, "resource value can't be null");

            if (threshold < 0 || threshold > 1) {
                throw new IllegalArgumentException("Resource value should be between 0 and 1");
            }
            String str = String.valueOf(threshold);
            if (str.contains(".") && str.split("\\.")[1].length() > 2) {
                throw new IllegalArgumentException("Resource value should have at most two digits after the decimal point");
            }
            if (!isValidResourceType((resourceName.toLowerCase(Locale.ROOT)))) {
                throw new IllegalArgumentException(
                    "resource has to be valid, valid resources are: " + Arrays.stream(ResourceType.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))
                );
            }
        }
    }

    /**
     * Verification for CreateQueryGroupRequest.updatedAtInMillis
     * @param updatedAt - updated time to be verified
     */
    public static void validateUpdatedAtInMillis(long updatedAt) {
        long minValidTimestamp = Instant.ofEpochMilli(0L).getMillis();
        long currentSeconds = Instant.now().getMillis();
        if (minValidTimestamp > updatedAt || updatedAt > currentSeconds) {
            throw new IllegalArgumentException("QueryGroup.updatedAtInMillis is not a valid epoch");
        }
    }

    @Override
    public CreateQueryGroupRequest read(StreamInput in) throws IOException {
        return new CreateQueryGroupRequest(in);
    }

    /**
     * Generate a CreateQueryGroupRequest from XContent
     * @param parser - A {@link XContentParser} object
     */
    public static CreateQueryGroupRequest fromXContent(XContentParser parser) throws IOException {

        while (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            parser.nextToken();
        }

        if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new IllegalArgumentException("expected start object but got a " + parser.currentToken());
        }

        XContentParser.Token token;
        String fieldName = "";
        String name = null;
        String _id = UUIDs.randomBase64UUID();
        ResiliencyMode mode = null;
        long updatedAtInMillis = Instant.now().getMillis();

        // Map to hold resources
        final Map<String, Object> resourceLimits = new HashMap<>();
        while ((token = parser.nextToken()) != null) {
            if (token == XContentParser.Token.FIELD_NAME) {
                fieldName = parser.currentName();
            } else if (token.isValue()) {
                if (fieldName.equals("name")) {
                    name = parser.text();
                } else if (fieldName.equals("resiliency_mode")) {
                    mode = ResiliencyMode.fromName(parser.text());
                } else {
                    throw new IllegalArgumentException("unrecognised [field=" + fieldName + " in QueryGroup");
                }
            } else if (token == XContentParser.Token.START_OBJECT) {
                if (!fieldName.equals("resourceLimits")) {
                    throw new IllegalArgumentException(
                        "QueryGroup.resourceLimits is an object and expected token was { " + " but found " + token
                    );
                }
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        fieldName = parser.currentName();
                    } else {
                        resourceLimits.put(fieldName, parser.doubleValue());
                    }
                }
            }
        }
        return new CreateQueryGroupRequest(name, _id, mode, resourceLimits, updatedAtInMillis);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * name getter
     */
    public String getName() {
        return name;
    }

    /**
     * name setter
     * @param name - name to be set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * resourceLimits getter
     */
    public Map<String, Object> getResourceLimits() {
        return resourceLimits;
    }

    /**
     * resourceLimits setter
     * @param resourceLimits - resourceLimit to be set
     */
    public void setResourceLimits(Map<String, Object> resourceLimits) {
        this.resourceLimits = resourceLimits;
    }

    /**
     * mode getter
     */
    public ResiliencyMode getResiliencyMode() {
        return resiliencyMode;
    }

    /**
     * mode setter
     * @param resiliencyMode - mode to be set
     */
    public void setResiliencyMode(ResiliencyMode resiliencyMode) {
        this.resiliencyMode = resiliencyMode;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(name);
        out.writeString(_id);
        out.writeString(resiliencyMode.getName());
        out.writeMap(resourceLimits);
        out.writeLong(updatedAtInMillis);
    }

    /**
     * _id getter
     */
    public String get_id() {
        return _id;
    }

    /**
     * UUID setter
     * @param _id - _id to be set
     */
    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * updatedAtInMillis getter
     */
    public long getUpdatedAtInMillis() {
        return updatedAtInMillis;
    }

    /**
     * updatedAtInMillis setter
     * @param updatedAtInMillis - updatedAtInMillis to be set
     */
    public void setUpdatedAtInMillis(long updatedAtInMillis) {
        this.updatedAtInMillis = updatedAtInMillis;
    }
}
