/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;


import org.opensearch.action.sandbox.CreateSandboxResponse;
import org.opensearch.action.sandbox.GetSandboxRequest;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;

/**
 * This interface defines the key APIs for implementing Sandbox persistence
 */
public interface Persistable<T> {
    /**
     * persists the @link Sandbox in a durable storage
     * @param sandbox
     */
    <U extends ActionResponse> void persist(T sandbox, ActionListener<U> listener);

    <U extends ActionResponse> void update(T sandbox, ActionListener<U> listener);

    <U extends ActionResponse> void get(String _id, ActionListener<U> listener);

    <U extends ActionResponse> void delete(String _id, ActionListener<U> listener);
}
