package org.blocks4j.commons.metrics3.id;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;

import java.util.Calendar;

public class MonthlyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private MonthlyMetricId(MetricType metricType) {
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
        calendar.set(Calendar.DAY_OF_MONTH, 0);

        return calendar.getTimeInMillis();
    }

    public static MonthlyMetricIdBuilder<Counter> createMonthlyCounterIdBuilder() {
        return new MonthlyMetricIdBuilder<>(MetricType.COUNTER);
    }

    public static MonthlyMetricIdBuilder<Meter> createMonthlyMeterIdBuilder() {
        return new MonthlyMetricIdBuilder<>(MetricType.METER);
    }

    public static MonthlyMetricIdBuilder<Timer> createMonthlyTimerIdBuilder() {
        return new MonthlyMetricIdBuilder<>(MetricType.TIMER);
    }

    public static class MonthlyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, MonthlyMetricId<METRIC>> {
        private MonthlyMetricIdBuilder(MetricType metricType) {
            super(new MonthlyMetricId<METRIC>(metricType));
        }
    }
}
