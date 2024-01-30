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
 * Delete Sandbox Action class
 */
public class DeleteSandboxAction extends ActionType<DeleteSandboxResponse> {
    public static final DeleteSandboxAction INSTANCE = new DeleteSandboxAction();
    public static final String NAME = "cluster:admin/sandbox/_delete";

    private DeleteSandboxAction() {
        super(NAME, DeleteSandboxResponse::new);
    }
}
