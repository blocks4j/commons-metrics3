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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.blocks4j.commons.metrics3.id.TemporalMetricId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricRepository {

    private static final Logger log = LoggerFactory.getLogger(MetricRepository.class);
    private final Thread shutdownHook;
    private final MetricRepositoryService repo;
    private final MetricCounterBackup backup;
    private final MetricRegistry registry;
    private long cleanUpInterval;

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private Locale locale;
        private String backupDirectory;
        private long cleanUpInterval;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.locale = Locale.getDefault();
            this.cleanUpInterval = TimeUnit.MINUTES.toMillis(1);
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

        public Builder withCleanUpInterval(long milliseconds) {
            this.cleanUpInterval = milliseconds;
            return this;
        }

        public MetricRepository build() {
            return new MetricRepository(this.registry, this.backupDirectory, this.locale, this.cleanUpInterval);
        }
    }

    private MetricRepository(MetricRegistry registry, String backupDirectory, Locale locale, long cleanUpInterval) {
        this.backup = StringUtils.isBlank(backupDirectory) ? MetricCounterBackup.noActionBackupService : new MetricCounterBackup(backupDirectory);
        this.registry = registry;
        this.repo = new MetricRepositoryService(this.registry, this.backup, locale);
        this.cleanUpInterval = cleanUpInterval;
        this.shutdownHook = getShutdownHook();
        getBackupThread().start();
        getCleanUpThread().start();
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
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
                while (true) {
                    try {
                        TimeUnit.MINUTES.sleep(1);
                        repo.backupCounters();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable throwable) {
                        log.error("Error in MetricRepository Backup.", throwable);
                    }
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
                while (true) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(MetricRepository.this.cleanUpInterval);
                        cleanUp();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable throwable) {
                        log.error("Error in MetricRepository Cleanup.", throwable);
                    }
                }
            }
        };
    }

    private void cleanUp() {
        for (MetricsRepositoryEntry metricsRepositoryEntry : this.repo.getKeys()) {
            purgeMetric(metricsRepositoryEntry);
        }
        this.backup.cleanup();
    }

    private void purgeMetric(MetricsRepositoryEntry metricsRepositoryEntry) {
        try {
            long lifetime = System.currentTimeMillis() - metricsRepositoryEntry.getReferenceTimestamp();

            if (lifetime > metricsRepositoryEntry.getMetricId().getExpiration()) {
                this.repo.remove(metricsRepositoryEntry);
            }

        } catch (Exception e) {
            log.warn("error while purging metrics", e);
        }
    }

    public <METRIC extends Metric, ID extends TemporalMetricId<METRIC>> METRIC getMetric(ID metricId) {
        return this.repo.getMetric(metricId);
    }


    public MetricRegistry getMetricRegistry() {
        return this.registry;
    }


    public Map<MetricsRepositoryEntry, Metric> getMetricRepository() {
        return this.repo.getMetricRepository();
    }
}
