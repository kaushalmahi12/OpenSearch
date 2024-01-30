/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import java.util.List;

/**
 * This interface is responsible for classifying incoming request into Sandboxes based on matching attributes in Sandbox and Request
 */
public interface RequestSandboxClassifier<T> {
    public List<Sandbox> classify(T request, List<Sandbox> availableSandboxes);
}
