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

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricRepository {

    private static final Logger log = LoggerFactory.getLogger(MetricRepository.class);
    private final int daysToKeep = 0;
    private final Thread shutdownHook;
    private final MetricRepositoryService repo;
    private final CounterBackupService backup;

    public MetricRepository(MetricRegistry registry, String locale) {
        this(registry, CounterBackupService.noActionBackupService, locale);
    }

    public MetricRepository(MetricRegistry registry, CounterBackupService backup, String locale) {
        this.backup = backup;
        repo = new MetricRepositoryService(registry, backup, locale);
        shutdownHook = getShutdownHook();
        getBackupThread().start();
        getCleanUpThread().start();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private Thread getShutdownHook() {
        return new Thread() {
            {
                setDaemon(true);
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
            }

            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
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
            }

            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
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
        for (Set<String> keys : repo.getKeys()) {
            purgeMetrics(keys, repo.getDateFormat(), daysToKeep);
        }
        backup.cleanup(daysToKeep);
    }

    private void purgeMetrics(Set<String> keys, FastDateFormat formatter, int daysToKeep) {
        for (String key : keys) {
            String[] fullName = key.split("\\|");
            if (fullName.length != 3) {
                continue;
            }

            try {
                Date meterDate = formatter.parse(fullName[2]);
                long daysBetween = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toDays(meterDate.getTime());

                if (daysBetween > daysToKeep) {
                    repo.remove(fullName, key);
                }

            } catch (Exception e) {
                log.warn("error while purging metrics", e);
                continue;
            }
        }
    }

    public Meter getDailyMeter(Class<?> klass, String name) {
        return repo.getDailyMeter(klass, name);
    }

    public Counter getDailyCounter(Class<?> klass, String name) {
        return repo.getDailyCounter(klass, name);
    }

    public Timer getDailyTimer(Class<?> klass, String name) {
        return repo.getDailyTimer(klass, name);
    }

    public Histogram getDailyHistogram(Class<?> klass, String name) {
        return repo.getDailyHistogram(klass, name);
    }


    public void removeAll() {
        for (Set<String> keys : repo.getKeys()) {
            purgeMetrics(keys, repo.getDateFormat(), -1);
        }
    }
}
