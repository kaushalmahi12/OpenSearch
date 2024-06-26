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
 * Rest action to get QueryGroup
 *
 * @opensearch.api
 */
public class GetQueryGroupAction extends ActionType<GetQueryGroupResponse> {

    /**
    /**
     * An instance of GetQueryGroupAction
     */
    public static final GetQueryGroupAction INSTANCE = new GetQueryGroupAction();

    /**
     * Name for GetQueryGroupAction
     */
    public static final String NAME = "cluster:admin/opensearch/query_group/wlm/_get";

    /**
     * Default constructor
     */
    private GetQueryGroupAction() {
        super(NAME, GetQueryGroupResponse::new);
    }
}
