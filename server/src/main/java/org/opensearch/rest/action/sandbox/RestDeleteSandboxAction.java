/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.sandbox;

import org.opensearch.action.sandbox.DeleteSandboxRequest;
import org.opensearch.action.sandbox.GetSandboxRequest;
import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.DELETE;

/**
 * Rest Action for DeleteSandbox Action
 */
public class RestDeleteSandboxAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(new Route(DELETE, "_sandbox/{_id}"), new Route(DELETE, "_sandbox"))
        );
    }

    @Override
    public String getName() {
        return "delete_sandbox";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String _id = request.param("_id");
        DeleteSandboxRequest deleteSandboxRequest = new DeleteSandboxRequest(_id);
//        request.applyContentParser((parser) -> {
//            parseRestRequest(deleteSandboxRequest, parser);
//        });
        return channel -> {
            client.deleteSandbox(deleteSandboxRequest, new RestStatusToXContentListener<>(channel));
        };
    }

//    private void parseRestRequest(DeleteSandboxRequest request, XContentParser parser) throws IOException {
//         final GetSandboxRequest getSandboxRequest = GetSandboxRequest.fromXContent(parser);
//         //request.setTags(getSandboxRequest.getTags());
//    }
}
