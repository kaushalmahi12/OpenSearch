/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.sandbox;

import org.opensearch.action.sandbox.UpdateSandboxRequest;
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
 * Rest Action for UpdateSandbox Action
 */
public class RestUpdateSandboxAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(POST, "_sandbox/{name}"), new Route(PUT, "_sandbox/{name}")));
    }

    @Override
    public String getName() {
        return "update_sandbox";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String name = request.param("name");
        UpdateSandboxRequest updateSandboxRequest = new UpdateSandboxRequest(name);
        request.applyContentParser((parser) -> { parseRestRequest(updateSandboxRequest, parser); });
        return channel -> { client.updateSandbox(updateSandboxRequest, new RestStatusToXContentListener<>(channel)); };
    }

    private void parseRestRequest(UpdateSandboxRequest request, XContentParser parser) throws IOException {
        final UpdateSandboxRequest updateSandboxRequest = UpdateSandboxRequest.fromXContent(parser);
        request.setUpdatingName(updateSandboxRequest.getUpdatingName());
        request.setSandboxAttributes(updateSandboxRequest.getSandboxAttributes());
        request.setResourceConsumptionLimits(updateSandboxRequest.getResourceConsumptionLimits());
        request.setEnforcement(updateSandboxRequest.getEnforcement());
    }
}
