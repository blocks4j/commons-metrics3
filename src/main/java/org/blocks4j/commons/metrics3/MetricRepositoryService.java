/*
 *   Copyright 2013-2015 Blocks4J Team (www.blocks4j.org)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.blocks4j.commons.metrics3;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.time.FastDateFormat;
import org.blocks4j.commons.metrics3.id.TemporalMetricId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class MetricRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(MetricRepositoryService.class);

    private final MetricCounterBackup backup;
    private final MetricRegistry registry;
    private final FastDateFormat dateFormat;

    private final ConcurrentMap<String, MetricsRepositoryEntry> metricsRepositoryIds;

    public MetricRepositoryService(MetricRegistry registry, MetricCounterBackup backup, Locale locale) {
        this.metricsRepositoryIds = new ConcurrentHashMap<>();
        this.registry = registry;
        this.backup = backup;
        this.dateFormat = FastDateFormat.getInstance("dd/MM/yyyy HH:mm'('EEE')'", locale);
    }

    public <METRIC extends Metric, ID extends TemporalMetricId<METRIC>> METRIC getMetric(ID metricId) {
        long referenceTimestamp = metricId.truncateTimestamp(System.currentTimeMillis());
        MetricsRepositoryEntry metricsRepositoryEntry = new MetricsRepositoryEntry(metricId, referenceTimestamp, this.dateFormat);

        synchronized (this) {
            METRIC metric = (METRIC) this.getMetric(metricsRepositoryEntry);

            if (metric == null) {
                metric = this.createNewMetric(metricsRepositoryEntry);
                this.metricsRepositoryIds.putIfAbsent(metricsRepositoryEntry.getRepositoryId(), metricsRepositoryEntry);
            }
            return metric;
        }
    }

    private <METRIC extends Metric> METRIC createNewMetric(MetricsRepositoryEntry metricsRepositoryEntry) {
        Metric metric;
        switch (metricsRepositoryEntry.getMetricId().getMetricType()) {
            case METER:
                metric = this.registry.meter(metricsRepositoryEntry.getRepositoryId());
                break;
            case COUNTER:
                String repositoryId = metricsRepositoryEntry.getRepositoryId();
                metric = this.registry.counter(repositoryId);
                this.loadBackup((Counter) metric, repositoryId);
                break;
            case TIMER:
                metric = this.registry.timer(metricsRepositoryEntry.getRepositoryId());
                break;
            default:
                throw new IllegalArgumentException("Unknown metric type.");
        }

        return (METRIC) metric;
    }

    private void loadBackup(Counter counter, String repositoryId) {
        long value = backup.get(repositoryId);
        if (value > 0 && counter.getCount() == 0) {
            counter.inc(value);
        }
    }

    public synchronized Metric getMetric(MetricsRepositoryEntry metricsRepositoryEntry) {
        return this.registry.getMetrics().get(metricsRepositoryEntry.getRepositoryId());
    }

    public void backupCounters() {
        for (MetricsRepositoryEntry metricsRepositoryEntry : metricsRepositoryIds.values()) {
            if (metricsRepositoryEntry.getMetricId().getMetricType() == MetricType.COUNTER) {
                try {
                    backup.persist(metricsRepositoryEntry.getRepositoryId(), ((Counter) this.getMetric(metricsRepositoryEntry)).getCount());
                } catch (Exception ignored) {
                    log.error("error while persisting counter", ignored);
                }
            }
        }
    }

    public List<MetricsRepositoryEntry> getKeys() {
        return new ArrayList<>(this.metricsRepositoryIds.values());
    }

    public void remove(TemporalMetricId<?> metricId, long referenceTimestamp) {
        this.remove(new MetricsRepositoryEntry(metricId, referenceTimestamp, this.dateFormat));
    }

    public synchronized void remove(MetricsRepositoryEntry entry) {
        String repositoryId = entry.getRepositoryId();

        this.metricsRepositoryIds.remove(repositoryId);
        this.registry.remove(repositoryId);
    }

    public Map<MetricsRepositoryEntry, Metric> getMetricRepository() {
        Map<MetricsRepositoryEntry, Metric> metricRepo = new HashMap<>();

        Set<Map.Entry<String, MetricsRepositoryEntry>> entries = new HashSet<>(this.metricsRepositoryIds.entrySet());

        for (Map.Entry<String, MetricsRepositoryEntry> idRepoEntry : entries) {
            Metric metric = this.getMetric(idRepoEntry.getValue());

            if (metric != null) {
                metricRepo.put(idRepoEntry.getValue(), metric);
            }

        }

        return Collections.unmodifiableMap(metricRepo);
    }
}
