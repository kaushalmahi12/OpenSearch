/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.UserPrincipleExtractor;

import java.util.List;

/**
 * This class is mainly used to classify co-ordinator search reqyests into user level sandboxes
 */
public class CoordinatorRequestSandboxClassifier implements RequestSandboxClassifier<SearchRequest> {

    private final UserPrincipleExtractor userPrincipleExtractor;

    public CoordinatorRequestSandboxClassifier(UserPrincipleExtractor userPrincipleExtractor) {
        this.userPrincipleExtractor = userPrincipleExtractor;
    }

    /**
     *
     * @param request is a coordinator request which
     * @return List of matching sandboxes based on user firing the request
     */
    @Override
    public List<Sandbox> classify(final SearchRequest request, List<Sandbox> availableSandboxes) {
        String user = userPrincipleExtractor.getUserPrincipleFor(request);
        final String USER_FIELD = "user";
        for (Sandbox sandbox: availableSandboxes) {
            boolean matches = true;
            for (Sandbox.SelectionAttribute selectionAttribute: sandbox.getSelectionAttributes()) {
                if (selectionAttribute.)
            }
        }
        return null;
    }
}
