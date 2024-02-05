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
 * Update Sandbox Action class
 */
public class UpdateSandboxAction extends ActionType<UpdateSandboxResponse> {
    public static final UpdateSandboxAction INSTANCE = new UpdateSandboxAction();
    public static final String NAME = "cluster:admin/sandbox/_update";

    private UpdateSandboxAction() {
        super(NAME, UpdateSandboxResponse::new);
    }
}
