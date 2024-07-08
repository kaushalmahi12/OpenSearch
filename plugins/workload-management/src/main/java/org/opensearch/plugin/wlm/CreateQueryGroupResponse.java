/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.ResourceType;

import java.io.IOException;
import java.util.Map;

/**
 * Response for the create API for QueryGroup
 *
 * @opensearch.internal
 */
public class CreateQueryGroupResponse extends ActionResponse implements ToXContent, ToXContentObject {
    private final QueryGroup queryGroup;
    private RestStatus restStatus;

    /**
     * Constructor for CreateQueryGroupResponse
     * @param queryGroup - The QueryGroup to be created
     * @param restStatus - The resStatus for the response
     */
    public CreateQueryGroupResponse(final QueryGroup queryGroup, RestStatus restStatus) {
        this.queryGroup = queryGroup;
        this.restStatus = restStatus;
    }

    /**
     * Constructor for CreateQueryGroupResponse
     * @param in - A {@link StreamInput} object
     */
    public CreateQueryGroupResponse(StreamInput in) throws IOException {
        queryGroup = new QueryGroup(in);
        restStatus = RestStatus.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        queryGroup.writeTo(out);
        RestStatus.writeTo(out, restStatus);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("name", queryGroup.getName());
        builder.field("resiliency_mode", queryGroup.getResiliencyMode().getName());
        // write resource limits
        builder.startObject("resourceLimits");
        Map<ResourceType, Object> resourceLimits = queryGroup.getResourceLimits();
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceLimits.containsKey(resourceType)) {
                builder.field(resourceType.getName(), resourceLimits.get(resourceType));
            }
        }
        builder.endObject();
        builder.field("updatedAt", queryGroup.getUpdatedAtInMillis());
        builder.endObject();
        return builder;
    }

    /**
     * queryGroup getter
     */
    public QueryGroup getQueryGroup() {
        return queryGroup;
    }

    /**
     * restStatus getter
     */
    public RestStatus getRestStatus() {
        return restStatus;
    }
}
