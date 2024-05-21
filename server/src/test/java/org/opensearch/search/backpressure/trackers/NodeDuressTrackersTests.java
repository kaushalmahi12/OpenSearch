/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.backpressure.trackers;

import org.opensearch.search.ResourceType;
import org.opensearch.test.OpenSearchTestCase;

import java.util.EnumMap;

public class NodeDuressTrackersTests extends OpenSearchTestCase {

    public void testNodeNotInDuress() {
        EnumMap<ResourceType, NodeDuressTrackers.NodeDuressTracker> map = new EnumMap<>(ResourceType.class) {{
                put(ResourceType.JVM, new NodeDuressTrackers.NodeDuressTracker(() -> false, () -> 2));
                put(ResourceType.CPU, new NodeDuressTrackers.NodeDuressTracker(() -> false, () -> 2));
        }};

        NodeDuressTrackers nodeDuressTrackers = new NodeDuressTrackers(map);

        assertFalse(nodeDuressTrackers.isNodeInDuress());
        assertFalse(nodeDuressTrackers.isNodeInDuress());
        assertFalse(nodeDuressTrackers.isNodeInDuress());
    }

    public void testNodeInDuressWhenHeapInDuress() {
        EnumMap<ResourceType, NodeDuressTrackers.NodeDuressTracker> map = new EnumMap<>(ResourceType.class) {
            {
                put(ResourceType.JVM, new NodeDuressTrackers.NodeDuressTracker(() -> true, () -> 3));
                put(ResourceType.CPU, new NodeDuressTrackers.NodeDuressTracker(() -> false, () -> 1));
            }};

        NodeDuressTrackers nodeDuressTrackers = new NodeDuressTrackers(map);

        assertFalse(nodeDuressTrackers.isNodeInDuress());
        assertFalse(nodeDuressTrackers.isNodeInDuress());

        // for the third time it should be in duress
        assertTrue(nodeDuressTrackers.isNodeInDuress());
    }

    public void testNodeInDuressWhenCPUInDuress() {
        EnumMap<ResourceType, NodeDuressTrackers.NodeDuressTracker> map = new EnumMap<>(ResourceType.class) {
            {
                put(ResourceType.JVM, new NodeDuressTrackers.NodeDuressTracker(() -> false, () -> 1));
                put(ResourceType.CPU, new NodeDuressTrackers.NodeDuressTracker(() -> true, () -> 3));
            }};

        NodeDuressTrackers nodeDuressTrackers = new NodeDuressTrackers(map);

        assertFalse(nodeDuressTrackers.isNodeInDuress());
        assertFalse(nodeDuressTrackers.isNodeInDuress());

        // for the third time it should be in duress
        assertTrue(nodeDuressTrackers.isNodeInDuress());
    }

    public void testNodeInDuressWhenCPUAndHeapInDuress() {
        EnumMap<ResourceType, NodeDuressTrackers.NodeDuressTracker> map = new EnumMap<>(ResourceType.class) {
            {
                put(ResourceType.JVM, new NodeDuressTrackers.NodeDuressTracker(() -> true, () -> 3));
                put(ResourceType.CPU, new NodeDuressTrackers.NodeDuressTracker(() -> false, () -> 3));
            }};

        NodeDuressTrackers nodeDuressTrackers = new NodeDuressTrackers(map);

        assertFalse(nodeDuressTrackers.isNodeInDuress());
        assertFalse(nodeDuressTrackers.isNodeInDuress());

        // for the third time it should be in duress
        assertTrue(nodeDuressTrackers.isNodeInDuress());
    }
}
