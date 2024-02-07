/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.common.UserPrincipleExtractor;
import org.opensearch.tasks.Task;

import java.util.List;

/**
 * This class is mainly used to classify co-ordinator search/multi-search reqyests into  sandboxes
 */
public class RequestSandboxClassifier {

    /**
     *
     * @param task is a coordinator request task
     * @return List of matching sandboxes based on user firing the request
     */
    public Sandbox classify(final Task task, List<Sandbox> availableSandboxes) {
        // TODO: maybe we don;t need the request to get user related info
        String user = UserPrincipleExtractor.getUserPrincipleFor(task);

        return null;
    }

}
