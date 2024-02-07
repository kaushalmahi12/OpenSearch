/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.common;


import org.opensearch.tasks.Task;

/**
 * This class is used to extract the user info from propagated from security layer
 */
public class UserPrincipleExtractor {
    // TODO: Add logic to extract the user principle per request for both co-ordinator request and shard level requests
    public static String getUserPrincipleFor(final Task task) {
        return null;
    }
}
