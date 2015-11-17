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
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.blocks4j.commons.metrics3.id.DailyMetricId;
import org.blocks4j.commons.metrics3.id.TemporalMetricId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MetricRepository {

    private static final Logger log = LoggerFactory.getLogger(MetricRepository.class);
    private final Thread shutdownHook;
    private final MetricRepositoryService repo;
    private final MetricCounterBackup backup;
    private final MetricRegistry registry;

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private Locale locale;
        private String backupDirectory;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.locale = Locale.getDefault();
        }

        public Builder formattedFor(Locale locale) {
            if (locale != null) {
                this.locale = locale;
            }
            return this;
        }

        public Builder formattedFor(String locale) {
            try {
                this.locale = LocaleUtils.toLocale(locale);
            } catch (Exception ignored) {
            }
            return this;
        }

        public Builder withBackup(String backupDirectory) {
            this.backupDirectory = backupDirectory;
            return this;
        }

        public MetricRepository build() {
            return new MetricRepository(registry, backupDirectory, locale);
        }
    }

    private MetricRepository(MetricRegistry registry, String backupDirectory, Locale locale) {
        this.backup = StringUtils.isBlank(backupDirectory) ? MetricCounterBackup.noActionBackupService : new MetricCounterBackup(backupDirectory);
        this.registry = registry;
        repo = new MetricRepositoryService(this.registry, backup, locale);
        shutdownHook = getShutdownHook();
        getBackupThread().start();
        getCleanUpThread().start();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private Thread getShutdownHook() {
        return new Thread() {
            {
                setDaemon(true);
                setName("MetricRepositoryShutdownHook");
            }

            public void run() {
                cleanUp();
                repo.backupCounters();
            }
        };
    }

    private Thread getBackupThread() {
        return new Thread() {
            {
                setDaemon(true);
                setName("MetricRepositoryBackupThread");
            }

            public void run() {
                try {
                    while (true) {
                        TimeUnit.MINUTES.sleep(1);
                        repo.backupCounters();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    private Thread getCleanUpThread() {
        return new Thread() {
            {
                setDaemon(true);
                setName("MetricRepositoryCleanUpThread");
            }

            public void run() {
                try {
                    while (true) {
                        TimeUnit.MINUTES.sleep(1);
                        cleanUp();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    private void cleanUp() {
        for (MetricsRepositoryEntry metricsRepositoryEntry : repo.getKeys()) {
            purgeMetric(metricsRepositoryEntry);
        }
        backup.cleanup();
    }

    private void purgeMetric(MetricsRepositoryEntry metricsRepositoryEntry) {
        try {
            long lifetime = System.currentTimeMillis() - metricsRepositoryEntry.getReferenceTimestamp();

            if (lifetime > metricsRepositoryEntry.getMetricId().getExpiration()) {
                repo.remove(metricsRepositoryEntry);
            }

        } catch (Exception e) {
            log.warn("error while purging metrics", e);
        }
    }

    public <METRIC extends Metric, ID extends TemporalMetricId<METRIC>> METRIC getMetric(ID metricId) {
        return repo.getMetric(metricId);
    }

    /**
     * @deprecated  replaced by {@link #getMetric(TemporalMetricId)}
     */
    @Deprecated
    public Meter dailyMeter(Class<?> klass, String name) {
        return this.getMetric(DailyMetricId.createDailyMeterIdBuilder()
                .name(name)
                .ownerClass(klass)
                .build());
    }

    /**
     * @deprecated  replaced by {@link #getMetric(TemporalMetricId)}
     */
    @Deprecated
    public Counter dailyCounter(Class<?> klass, String name) {
        return this.getMetric(DailyMetricId.createDailyCounterIdBuilder()
                .name(name)
                .ownerClass(klass)
                .build());
    }

    /**
     * @deprecated  replaced by {@link #getMetric(TemporalMetricId)}
     */
    @Deprecated
    public Timer dailyTimer(Class<?> klass, String name) {
        return this.getMetric(DailyMetricId.createDailyTimerIdBuilder()
                .name(name)
                .ownerClass(klass)
                .build());
    }

    /**
     * @deprecated  replaced by {@link #getMetric(TemporalMetricId)}
     */
    @Deprecated
    public Histogram dailyHistogram(Class<?> klass, String name) {
        return this.getMetric(DailyMetricId.createDailyHistogramIdBuilder()
                .name(name)
                .ownerClass(klass)
                .build());
    }

    public MetricRegistry getMetricRegistry() {
        return this.registry;
    }
}
