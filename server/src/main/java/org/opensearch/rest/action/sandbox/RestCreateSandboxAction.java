/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.sandbox;

import org.opensearch.action.sandbox.CreateSandboxRequest;
import org.opensearch.client.node.NodeClient;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

/**
 * Rest Action for CreateSandbox Action
 */
public class RestCreateSandboxAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(POST, "_sandbox/"), new Route(PUT, "_sandbox/")));
    }

    @Override
    public String getName() {
        return "create_sandbox";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        CreateSandboxRequest createSandboxRequest = new CreateSandboxRequest();
        request.applyContentParser((parser) -> { parseRestRequest(createSandboxRequest, parser); });
        return channel -> { client.createSandbox(createSandboxRequest, new RestStatusToXContentListener<>(channel)); };
    }

    private void parseRestRequest(CreateSandboxRequest request, XContentParser parser) throws IOException {
        final CreateSandboxRequest createSandboxRequest = CreateSandboxRequest.fromXContent(parser);
        request.setName(createSandboxRequest.getName());
        request.setSandboxAttributes(createSandboxRequest.getSandboxAttributes());
        request.setResourceConsumptionLimits(createSandboxRequest.getResourceConsumptionLimits());
        request.setEnforcement(createSandboxRequest.getEnforcement());
    }
}
