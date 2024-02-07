/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.unit.TimeValue;

public class QuerySandboxServiceSettings {
    private static final Long DEFAULT_RUN_INTERVAL_MILLIS = 1000l;

    private TimeValue runIntervalMillis;
    private Setting<Long> runIntervalSetting = Setting.longSetting(
        "query_sandbpx_service.run_interval_millis",
        DEFAULT_RUN_INTERVAL_MILLIS,
        1,
        Setting.Property.NodeScope
        );

    public QuerySandboxServiceSettings(Settings settings) {
        runIntervalMillis = new TimeValue(runIntervalSetting.get(settings));
    }

    public TimeValue getRunIntervalMillis() {
        return runIntervalMillis;
    }
}
