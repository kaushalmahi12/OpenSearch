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

import java.io.IOException;
import java.util.List;

/**
 * Response for the delete API for QueryGroup
 *
 * @opensearch.internal
 */
public class DeleteQueryGroupResponse extends ActionResponse implements ToXContent, ToXContentObject {
    private final List<QueryGroup> queryGroups;
    private RestStatus restStatus;

    /**
     * Constructor for DeleteQueryGroupResponse
     * @param queryGroups - The QueryGroup list to be fetched
     * @param restStatus - The rest status for this response
     */
    public DeleteQueryGroupResponse(final List<QueryGroup> queryGroups, RestStatus restStatus) {
        this.queryGroups = queryGroups;
        this.restStatus = restStatus;
    }

    /**
     * Constructor for DeleteQueryGroupResponse
     * @param in - A {@link StreamInput} object
     */
    public DeleteQueryGroupResponse(StreamInput in) throws IOException {
        this.queryGroups = in.readList(QueryGroup::new);
        this.restStatus = RestStatus.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(queryGroups);
        RestStatus.writeTo(out, restStatus);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray("deleted");
        for (QueryGroup group : queryGroups) {
            builder.startObject();
            builder.field("name", group.getName());
            builder.field("mode", group.getMode().getName());
            builder.field("updatedAt", group.getUpdatedAtInMillis());
            builder.mapContents(group.getResourceLimits());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    /**
     * queryGroups getter
     */
    public List<QueryGroup> getQueryGroups() {
        return queryGroups;
    }

    /**
     * restStatus getter
     */
    public RestStatus getRestStatus() {
        return restStatus;
    }

    /**
     * restStatus setter
     * @param restStatus - A {@link RestStatus} object
     */
    public void setRestStatus(RestStatus restStatus) {
        this.restStatus = restStatus;
    }
}
