/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.sandbox;

import org.opensearch.action.sandbox.GetSandboxRequest;
import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.GET;

/**
 * Rest Action for GetSandbox Action
 */
public class RestGetSandboxAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(new Route(GET, "_sandbox/{name}"), new Route(GET, "_sandbox/"))
        );
    }

    @Override
    public String getName() {
        return "get_sandbox";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String name = request.param("name");
        GetSandboxRequest getSandboxRequest = new GetSandboxRequest(name);
        return channel -> {
            client.getSandbox(getSandboxRequest, new RestStatusToXContentListener<>(channel));
        };
    }
}
