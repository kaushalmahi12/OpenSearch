/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm.action;

import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.QueryGroup;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugin.wlm.action.service.QueryGroupPersistenceService;
import org.opensearch.search.ResourceType;
import org.opensearch.threadpool.ThreadPool;

import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;

import static org.opensearch.cluster.metadata.QueryGroup.builder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opensearch.search.ResourceType.fromName;

public class QueryGroupTestUtils {
    public static final String NAME_ONE = "query_group_one";
    public static final String NAME_TWO = "query_group_two";
    public static final String _ID_ONE = "AgfUO5Ja9yfsYlONlYi3TQ==";
    public static final String _ID_TWO = "G5iIqHy4g7eK1qIAAAAIH53=1";
    public static final String NAME_NONE_EXISTED = "query_group_none_existed";
    public static final String MEMORY_STRING = "memory";
    public static final String MONITOR_STRING = "monitor";
    public static final long TIMESTAMP_ONE = 4513232413L;
    public static final long TIMESTAMP_TWO = 4513232415L;
    public static final QueryGroup queryGroupOne = builder().name(NAME_ONE)
        ._id(_ID_ONE)
        .mode(MONITOR_STRING)
        .resourceLimits(Map.of(fromName(MEMORY_STRING), 0.3))
        .updatedAt(TIMESTAMP_ONE)
        .build();

    public static final QueryGroup queryGroupTwo = builder().name(NAME_TWO)
        ._id(_ID_TWO)
        .mode(MONITOR_STRING)
        .resourceLimits(Map.of(fromName(MEMORY_STRING), 0.6))
        .updatedAt(TIMESTAMP_TWO)
        .build();

    public static final Map<String, QueryGroup> queryGroupMap = Map.of(NAME_ONE, queryGroupOne, NAME_TWO, queryGroupTwo);

    public static List<QueryGroup> queryGroupList() {
        List<QueryGroup> list = new ArrayList<>();
        list.add(queryGroupOne);
        list.add(queryGroupTwo);
        return list;
    }

    public static ClusterState clusterState() {
        final Metadata metadata = Metadata.builder().queryGroups(Map.of(NAME_ONE, queryGroupOne, NAME_TWO, queryGroupTwo)).build();
        return ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
    }

    public static Settings settings() {
        return Settings.builder().build();
    }

    public static ClusterSettings clusterSettings() {
        return new ClusterSettings(settings(), ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
    }

    public static QueryGroupPersistenceService queryGroupPersistenceService() {
        ClusterService clusterService = new ClusterService(settings(), clusterSettings(), mock(ThreadPool.class));
        return new QueryGroupPersistenceService(clusterService, settings(), clusterSettings());
    }

    public static List<Object> prepareSandboxPersistenceService(Map<String, QueryGroup> queryGroups) {
        Metadata metadata = Metadata.builder().queryGroups(queryGroups).build();
        Settings settings = Settings.builder().build();
        ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        ClusterService clusterService = new ClusterService(settings, clusterSettings, mock(ThreadPool.class));
        ClusterState clusterState = ClusterState.builder(new ClusterName("_name")).metadata(metadata).build();
        QueryGroupPersistenceService queryGroupPersistenceService = new QueryGroupPersistenceService(
            clusterService,
            settings,
            clusterSettings
        );
        return List.of(queryGroupPersistenceService, clusterState);
    }

    public static void compareResourceTypes(Map<ResourceType, Object> resourceLimitMapOne, Map<ResourceType, Object> resourceLimitMapTwo) {
        assertTrue(resourceLimitMapOne.keySet().containsAll(resourceLimitMapTwo.keySet()));
        assertTrue(resourceLimitMapOne.values().containsAll(resourceLimitMapTwo.values()));
//        for (Map.Entry<ResourceType, Object> entryOne : resourceLimitMapOne.entrySet()) {
//            String resourceName = entryOne.getKey().getName();
//            Optional<Map.Entry<ResourceType, Object>> entryTwo = resourceLimitMapTwo.entrySet().stream()
//                .filter(e -> e.getKey().getName().equals(resourceName))
//                .findFirst();
//            assertTrue(entryTwo.isPresent());
//            assertEquals(entryOne.getValue(), entryTwo.get().getValue());
//        }
    }

    public static void compareResourceLimits(Map<String, Object> resourceLimitMapOne, Map<String, Object> resourceLimitMapTwo) {
        assertTrue(resourceLimitMapOne.keySet().containsAll(resourceLimitMapTwo.keySet()));
        assertTrue(resourceLimitMapOne.values().containsAll(resourceLimitMapTwo.values()));
    }

    public static void compareQueryGroups(List<QueryGroup> listOne, List<QueryGroup> listTwo) {
        assertEquals(listOne.size(), listTwo.size());
        listOne.sort(Comparator.comparing(QueryGroup::getName));
        listTwo.sort(Comparator.comparing(QueryGroup::getName));
        for (int i = 0; i < listOne.size(); i++) {
            assertTrue(listOne.get(i).equals(listTwo.get(i)));
        }
    }

    public static void assertInflightValuesAreZero(QueryGroupPersistenceService queryGroupPersistenceService) {
        assertEquals(0, queryGroupPersistenceService.getInflightCreateQueryGroupRequestCount().get());
        Map<String, DoubleAdder> inflightResourceMap = queryGroupPersistenceService.getInflightResourceLimitValues();
        if (inflightResourceMap != null) {
            for (String resourceName : inflightResourceMap.keySet()) {
                assertEquals(0, inflightResourceMap.get(resourceName).intValue());
            }
        }
    }
}
