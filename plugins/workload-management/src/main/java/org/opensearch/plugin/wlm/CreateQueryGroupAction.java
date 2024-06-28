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
 * Rest action to create QueryGroup
 *
 * @opensearch.api
 */
public class CreateQueryGroupAction extends ActionType<CreateQueryGroupResponse> {

    /**
     * An instance of CreateQueryGroupAction
     */
    public static final CreateQueryGroupAction INSTANCE = new CreateQueryGroupAction();

    /**
     * Name for CreateQueryGroupAction
     */
    public static final String NAME = "cluster:admin/opensearch/query_group/wlm/_create";

    /**
     * Default constructor
     */
    private CreateQueryGroupAction() {
        super(NAME, CreateQueryGroupResponse::new);
    }
}
