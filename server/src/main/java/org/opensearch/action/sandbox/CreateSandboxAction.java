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
 * Create Sandbox Action class
 */
public class CreateSandboxAction extends ActionType<CreateSandboxResponse> {
    public static final CreateSandboxAction INSTANCE = new CreateSandboxAction();
    public static final String NAME = "cluster:admin/sandbox/_create";

    private CreateSandboxAction() {
        super(NAME, CreateSandboxResponse::new);
    }
}
