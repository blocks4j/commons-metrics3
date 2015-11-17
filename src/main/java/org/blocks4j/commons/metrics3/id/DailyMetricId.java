package org.blocks4j.commons.metrics3.id;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.blocks4j.commons.metrics3.MetricType;
import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DailyMetricId<METRIC extends Metric> extends TemporalMetricId<METRIC> {

    private DailyMetricId(MetricType metricType) {
        this.setMetricType(metricType);
    }

    @Override
    public long truncateTimestamp(long timestamp) {
        return new DateTime(timestamp).withTimeAtStartOfDay().getMillis();
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

    public static DailyMetricIdBuilder<Histogram> createDailyHistogramIdBuilder() {
        return new DailyMetricIdBuilder<>(MetricType.HISTOGRAM);
    }

    public static class DailyMetricIdBuilder<METRIC extends Metric> extends TemporalMetricIdBuilder<METRIC, DailyMetricId<METRIC>> {
        private DailyMetricIdBuilder(MetricType metricType) {
            super(new DailyMetricId<METRIC>(metricType));
            this.expiration(TimeUnit.DAYS.toMillis(2));
        }

        @Override
        public TemporalMetricIdBuilder<METRIC, DailyMetricId<METRIC>> expiration(long expiration) {
            if(expiration < TimeUnit.DAYS.toMillis(1)){
                throw new IllegalStateException("Is this configuration right?");
            }

            return super.expiration(expiration);
        }
    }
}
