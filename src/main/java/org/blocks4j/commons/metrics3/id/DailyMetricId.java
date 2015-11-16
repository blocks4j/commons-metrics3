package org.blocks4j.commons.metrics3.id;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;

import java.util.Calendar;

public class DailyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private DailyMetricId(MetricType metricType) {
        this.setMetricType(metricType);
    }

    @Override
    public long truncateTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        return calendar.getTimeInMillis();
    }

    public static DailyMetricIdBuilder<Counter> createDailyCounterIdBuilder() {
        return new DailyMetricIdBuilder<>(MetricType.COUNTER);
    }

    public static DailyMetricIdBuilder<Meter> createDailyMeterIdBuilder() {
        return new DailyMetricIdBuilder<>(MetricType.METER);
    }

    public static DailyMetricIdBuilder<Timer> createDailyTimerIdBuilder() {
        return new DailyMetricIdBuilder<>(MetricType.TIMER);
    }

    public static class DailyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, DailyMetricId<METRIC>> {
        private DailyMetricIdBuilder(MetricType metricType) {
            super(new DailyMetricId<METRIC>(metricType));
        }
    }
}
