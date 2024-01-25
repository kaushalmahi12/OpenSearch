/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.action.ActionType;

/**
 * Get Sandbox Action class
 */
public class GetSandboxAction extends ActionType<GetSandboxResponse> {
    public static final GetSandboxAction INSTANCE = new GetSandboxAction();
    public static final String NAME = "cluster:admin/sandbox/_get";

    private GetSandboxAction() {
        super(NAME, GetSandboxResponse::new);
    }
}
