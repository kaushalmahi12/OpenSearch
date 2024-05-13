/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandboxing.cancellation;

import org.opensearch.search.sandboxing.SandboxResourceType;
import org.opensearch.tasks.Task;

import java.util.List;

public interface TaskCancellationStrategy {
    /**
     * Determines which tasks should be cancelled based on the provided criteria.
     * @param tasks List of tasks available for cancellation.
     * @param reduceBy The amount of resources that need to be reduced, guiding the selection process.
     * @return List of TaskCancellation detailing which tasks should be cancelled.
     */
    List<Task> selectTasksToCancel(List<Task> tasks, long reduceBy, SandboxResourceType resourceType);
}
