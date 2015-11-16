package org.blocks4j.commons.metrics3.id;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;

import java.util.concurrent.TimeUnit;

public class HourlyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private HourlyMetricId(MetricType metricType) {
        this.setMetricType(metricType);
    }

    @Override
    public long truncateTimestamp(long timestamp) {
        return (timestamp / TimeUnit.HOURS.toMillis(1)) * TimeUnit.HOURS.toMillis(1);
    }

    public static HourlyMetricIdBuilder<Counter> createHourlyCounterIdBuilder() {
        return new HourlyMetricIdBuilder<>(MetricType.COUNTER);
    }

    public static HourlyMetricIdBuilder<Meter> createHourlyMeterIdBuilder() {
        return new HourlyMetricIdBuilder<>(MetricType.METER);
    }

    public static HourlyMetricIdBuilder<Timer> createHourlyTimerIdBuilder() {
        return new HourlyMetricIdBuilder<>(MetricType.TIMER);
    }

    public static class HourlyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, HourlyMetricId<METRIC>> {
        private HourlyMetricIdBuilder(MetricType metricType) {
            super(new HourlyMetricId<METRIC>(metricType));
        }
    }
}
