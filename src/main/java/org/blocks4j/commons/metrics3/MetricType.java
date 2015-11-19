package org.blocks4j.commons.metrics3;

public enum MetricType {

    COUNTER("Counter"),
    METER("Meter"),
    TIMER("Timer"),
    HISTOGRAM("Histogram");

    private final String name;

    MetricType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
