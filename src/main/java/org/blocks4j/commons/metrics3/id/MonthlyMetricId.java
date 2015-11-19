package org.blocks4j.commons.metrics3.id;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.concurrent.TimeUnit;

public class MonthlyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private MonthlyMetricId(MetricType metricType) {
        this.setMetricType(metricType);
    }

    @Override
    public long truncateTimestamp(long timestamp) {
        LocalDate monthFirst = new DateTime(timestamp).toLocalDate().withDayOfMonth(1);
        return monthFirst.toDateTimeAtStartOfDay().getMillis();
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

    public static MonthlyMetricIdBuilder<Histogram> createMonthlyHistogramIdBuilder() {
        return new MonthlyMetricIdBuilder<>(MetricType.HISTOGRAM);
    }

    public static class MonthlyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, MonthlyMetricId<METRIC>> {
        private MonthlyMetricIdBuilder(MetricType metricType) {
            super(new MonthlyMetricId<METRIC>(metricType));
            this.expiration(TimeUnit.DAYS.toMillis(32));
        }

        @Override
        public TemporalMetricIdBuilder<METRIC, MonthlyMetricId<METRIC>> expiration(long expiration) {
            if(expiration < TimeUnit.DAYS.toMillis(31)){
                throw new IllegalStateException("Is this configuration right?");
            }

            return super.expiration(expiration);
        }
    }
}
