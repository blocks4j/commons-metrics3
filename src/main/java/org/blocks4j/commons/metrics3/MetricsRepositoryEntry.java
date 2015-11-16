package org.blocks4j.commons.metrics3;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.blocks4j.commons.metrics3.id.TemporalMetricId;

public class MetricsRepositoryEntry {
    private final TemporalMetricId<?> metricId;
    private final long referenceTimestamp;
    private final FastDateFormat dateFormat;
    private String repositoryId;

    public MetricsRepositoryEntry(TemporalMetricId<?> metricId, long referenceTimestamp, FastDateFormat dateFormat) {
        this.metricId = metricId;
        this.referenceTimestamp = referenceTimestamp;
        this.dateFormat = dateFormat;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (!(object instanceof MetricsRepositoryEntry)) {
            return false;
        }

        MetricsRepositoryEntry that = (MetricsRepositoryEntry) object;

        return ObjectUtils.equals(this.metricId, that.metricId) &&
                ObjectUtils.equals(this.referenceTimestamp, that.referenceTimestamp);
    }

    @Override
    public int hashCode() {
        long result = getMetricId() != null ? getMetricId().hashCode() : 0;
        result = 31 * result + getReferenceTimestamp();
        return (int) result;
    }

    public TemporalMetricId<?> getMetricId() {
        return this.metricId;
    }

    public long getReferenceTimestamp() {
        return this.referenceTimestamp;
    }

    public String getRepositoryId() {
        if (this.repositoryId == null) {
            this.repositoryId = MetricRegistry.name(metricId.getOwnerClass(),
                    metricId.getMetricType().getName(),
                    metricId.getName(),
                    dateFormat.format(this.referenceTimestamp));
        }
        return repositoryId;
    }
}