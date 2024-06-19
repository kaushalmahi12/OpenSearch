/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.opensearch.action.ActionType;

/**
 * Rest action to delete QueryGroup
 *
 * @opensearch.api
 */
public class DeleteQueryGroupAction extends ActionType<DeleteQueryGroupResponse> {

    /**
     /**
     * An instance of DeleteQueryGroupAction
     */
    public static final DeleteQueryGroupAction INSTANCE = new DeleteQueryGroupAction();

    /**
     * Name for DeleteQueryGroupAction
     */
    public static final String NAME = "cluster:admin/opensearch/query_group/wlm/_delete";

    /**
     * Default constructor
     */
    private DeleteQueryGroupAction() {
        super(NAME, DeleteQueryGroupResponse::new);
    }
}
