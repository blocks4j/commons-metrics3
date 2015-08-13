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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

final class MetricRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(MetricRepositoryService.class);
    private final Object lockCounter = new Object();
    private final Object lockMeter = new Object();
    private final Object lockTimer = new Object();
    private final Object lockHistogram = new Object();

    private final ConcurrentMap<String, Meter> meters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Histogram> histograms = new ConcurrentHashMap<>();

    private final MetricCounterBackup backup;
    private final MetricRegistry registry;
    private final FastDateFormat dateFormat;

    public MetricRepositoryService(MetricRegistry registry, MetricCounterBackup backup, Locale locale) {
        this.registry = registry;
        this.backup = backup;
        this.dateFormat = FastDateFormat.getInstance("dd/MM/yyyy '('EEE')'", locale);
    }

    public Meter getDailyMeter(Class<?> klass, String name) {
        String granularity = getDay();
        String metricName = getMetricName(klass, name, granularity);

        Meter result = meters.get(metricName);
        if (null != result) {
            return result;
        }

        synchronized (lockMeter) {
            try {
                meters.putIfAbsent(metricName, registry.meter(MetricRegistry.name(klass, "meter", name + "_" + granularity)));
            } catch (Exception ignored) {
                log.error("error while inserting new meter", ignored);
            }
        }

        result = meters.get(metricName);
        return result != null ? result : registry.meter("fallback");
    }

    public Counter getDailyCounter(Class<?> klass, String name) {
        String granularity = getDay();
        String metricName = getMetricName(klass, name, granularity);

        Counter result = counters.get(metricName);
        if (null != result) {
            return result;
        }

        synchronized (lockCounter) {
            try {
                Counter counter = registry.counter(MetricRegistry.name(klass, "counter", name + "_" + granularity));

                long value = backup.get(metricName);
                if (value > 0 && counter.getCount() == 0) {
                    counter.inc(value);
                }
                counters.putIfAbsent(metricName, counter);

            } catch (Exception ignored) {
                log.error("error while inserting new counter", ignored);
            }
        }

        result = counters.get(metricName);
        return result != null ? result : registry.counter("fallback");
    }

    public void backupCounters() {
        for (Entry<String, Counter> each : counters.entrySet()) {
            try {
                backup.persist(each.getKey(), each.getValue().getCount());
            } catch (Exception ignored) {
                log.error("error while persisting counter", ignored);
            }
        }
    }

    public Timer getDailyTimer(Class<?> klass, String name) {
        String day = getDay();
        String metricName = getMetricName(klass, name, day);
        Timer result = timers.get(metricName);
        if (null != result) {
            return result;
        }

        synchronized (lockTimer) {
            try {
                timers.putIfAbsent(metricName, registry.timer(MetricRegistry.name(klass, "timer", name + "_" + day)));
            } catch (Exception ignored) {
                log.error("error while inserting new timer", ignored);
            }
        }

        result = timers.get(metricName);
        return result != null ? result : registry.timer("fallback");
    }

    public Histogram getDailyHistogram(Class<?> klass, String name) {
        String day = getDay();
        String metricName = getMetricName(klass, name, day);
        Histogram result = histograms.get(metricName);
        if (null != result) {
            return result;
        }

        synchronized (lockHistogram) {
            try {
                histograms.putIfAbsent(metricName, registry.histogram(MetricRegistry.name(klass, "histogram", name + "_" + day)));
            } catch (Exception ignored) {
                log.error("error while inserting new histogram", ignored);
            }
        }

        result = histograms.get(metricName);
        return result != null ? result : registry.histogram("fallback");
    }

    public List<Set<String>> getKeys() {
        return Arrays.asList(meters.keySet(), timers.keySet(), counters.keySet(), histograms.keySet());
    }

    public void remove(String[] fullName, String key) {
        try {
            registry.remove(MetricRegistry.name(Class.forName(fullName[0]), fullName[1] + "_" + fullName[2]));
            counters.remove(key);
            meters.remove(key);
            timers.remove(key);
            histograms.remove(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FastDateFormat getDateFormat() {
        return dateFormat;
    }

    private String getDay() {
        String day = dateFormat.format(System.currentTimeMillis());
        return day;
    }

    private String getMetricName(Class<?> klass, String name, String ts) {
        return String.format("%s|%s|%s", klass.getName(), name, ts);
    }

}
