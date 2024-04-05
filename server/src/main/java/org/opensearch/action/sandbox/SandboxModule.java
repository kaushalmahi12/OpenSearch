/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.sandbox;

import org.opensearch.common.inject.AbstractModule;
import org.opensearch.common.inject.TypeLiteral;
import org.opensearch.search.sandbox.Persistable;
import org.opensearch.search.sandbox.Sandbox;
import org.opensearch.search.sandbox.SandboxPersistenceService;

/**
 * Guice Module to manage Query Sandboxing related objects
 */
public class SandboxModule extends AbstractModule {
    @Override
    protected void configure() {
        // bind(Persistable.class).to(SandboxPersistenceService.class).asEagerSingleton();
        bind(new TypeLiteral<Persistable<Sandbox>>() {
        }).to(SandboxPersistenceService.class).asEagerSingleton();

    }
}
