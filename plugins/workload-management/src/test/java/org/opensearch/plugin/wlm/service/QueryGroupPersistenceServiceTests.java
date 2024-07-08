/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm.service;

import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.search.ResourceType;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.opensearch.cluster.metadata.QueryGroup.builder;
import static org.mockito.Mockito.mock;
import static org.opensearch.plugin.wlm.QueryGroupTestUtils.*;
import static org.opensearch.search.query_group.QueryGroupServiceSettings.QUERY_GROUP_COUNT_SETTING_NAME;

public class QueryGroupPersistenceServiceTests extends OpenSearchTestCase {

    public void testCreateQueryGroup() {
        List<Object> setup = prepareSandboxPersistenceService(new ArrayList<>());
        QueryGroupPersistenceService queryGroupPersistenceService1 = (QueryGroupPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        ClusterState newClusterState = queryGroupPersistenceService1.saveQueryGroupInClusterState(queryGroupOne, clusterState);
        Map<String, QueryGroup> updatedGroupsMap = newClusterState.getMetadata().queryGroups();
        assertEquals(1, updatedGroupsMap.size());
        assertTrue(updatedGroupsMap.containsKey(NAME_ONE));
        compareQueryGroups(List.of(queryGroupOne), List.of(updatedGroupsMap.get(NAME_ONE)));
        assertInflightValuesAreZero(queryGroupPersistenceService());
    }

    public void testCreateAnotherQueryGroup() {
        List<Object> setup = prepareSandboxPersistenceService(List.of(queryGroupOne));
        QueryGroupPersistenceService queryGroupPersistenceService1 = (QueryGroupPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        ClusterState newClusterState = queryGroupPersistenceService1.saveQueryGroupInClusterState(queryGroupTwo, clusterState);
        Map<String, QueryGroup> updatedGroups = newClusterState.getMetadata().queryGroups();
        assertEquals(2, updatedGroups.size());
        compareQueryGroups(queryGroupList(), (List<QueryGroup>) updatedGroups.values());
        assertInflightValuesAreZero(queryGroupPersistenceService());
    }

    public void testCreateQueryGroupDuplicateName() {
        List<Object> setup = prepareSandboxPersistenceService(List.of(queryGroupOne));
        QueryGroupPersistenceService queryGroupPersistenceService1 = (QueryGroupPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        QueryGroup toCreate = builder().name(NAME_ONE)
            ._id("W5iIqHyhgi4K1qIAAAAIHw==")
            .mode(MONITOR_STRING)
            .resourceLimits(Map.of(ResourceType.fromName(MEMORY_STRING),  0.3))
            .updatedAt(1690934400000L)
            .build();
        assertThrows(RuntimeException.class, () -> queryGroupPersistenceService1.saveQueryGroupInClusterState(toCreate, clusterState));
    }

    public void testCreateQueryGroupOverflowAllocation() {
        List<Object> setup = prepareSandboxPersistenceService(List.of(queryGroupTwo));
        QueryGroup toCreate = builder().name(NAME_TWO)
            ._id("W5iIqHyhgi4K1qIAAAAIHw==")
            .mode(MONITOR_STRING)
            .resourceLimits(Map.of(ResourceType.fromName(MEMORY_STRING),  0.5))
            .updatedAt(1690934400000L)
            .build();
        QueryGroupPersistenceService queryGroupPersistenceService1 = (QueryGroupPersistenceService) setup.get(0);
        ClusterState clusterState = (ClusterState) setup.get(1);
        assertThrows(RuntimeException.class, () -> queryGroupPersistenceService1.saveQueryGroupInClusterState(toCreate, clusterState));
    }

    public void testCreateQueryGroupOverflowCount() {
        QueryGroup toCreate = builder().name(NAME_NONE_EXISTED)
            ._id("W5iIqHyhgi4K1qIAAAAIHw==")
            .mode(MONITOR_STRING)
            .resourceLimits(Map.of(ResourceType.fromName(MEMORY_STRING),  0.5))
            .updatedAt(1690934400000L)
            .build();
        Metadata metadata = Metadata.builder().queryGroups(queryGroupMap).build();
        Settings settings = Settings.builder().put(QUERY_GROUP_COUNT_SETTING_NAME, 2).build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        QueryGroupPersistenceService queryGroupPersistenceService1 = new QueryGroupPersistenceService(
            clusterService,
            settings,
            clusterSettings
        );
        assertThrows(RuntimeException.class, () -> queryGroupPersistenceService1.saveQueryGroupInClusterState(toCreate, clusterState));
    }
}
