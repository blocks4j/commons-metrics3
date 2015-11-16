package org.blocks4j.commons.metrics3.id;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;

import java.util.concurrent.TimeUnit;

public class MinutelyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private MinutelyMetricId(MetricType metricType) {
        this.setMetricType(metricType);
    }

    @Override
    public long truncateTimestamp(long timestamp) {
        return (timestamp / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
    }

    public static MinutelyMetricIdBuilder<Counter> createMinutelyCounterIdBuilder() {
        return new MinutelyMetricIdBuilder<>(MetricType.COUNTER);
    }

    public static MinutelyMetricIdBuilder<Meter> createMinutelyMeterIdBuilder() {
        return new MinutelyMetricIdBuilder<>(MetricType.METER);
    }

    public static MinutelyMetricIdBuilder<Timer> createMinutelyTimerIdBuilder() {
        return new MinutelyMetricIdBuilder<>(MetricType.TIMER);
    }

    public static class MinutelyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, MinutelyMetricId<METRIC>> {
        private MinutelyMetricIdBuilder(MetricType metricType) {
            super(new MinutelyMetricId<METRIC>(metricType));
        }
    }
}
