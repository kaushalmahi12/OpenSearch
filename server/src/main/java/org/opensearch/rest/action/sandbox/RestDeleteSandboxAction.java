/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.sandbox;

import org.opensearch.action.sandbox.DeleteSandboxRequest;
import org.opensearch.client.node.NodeClient;
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
        return unmodifiableList(asList(new Route(DELETE, "_sandbox/{name}"), new Route(DELETE, "_sandbox")));
    }

    @Override
    public String getName() {
        return "delete_sandbox";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String name = request.param("name");
        DeleteSandboxRequest deleteSandboxRequest = new DeleteSandboxRequest(name);
        return channel -> { client.deleteSandbox(deleteSandboxRequest, new RestStatusToXContentListener<>(channel)); };
    }
}
