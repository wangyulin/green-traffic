package com.greentraffic.core.port.output;

import com.greentraffic.core.domain.traffic.TrafficMetric;

import java.util.List;

/**
 * Port for writing domain `TrafficMetric` to an external sink (InfluxDB, VictoriaMetrics, ...)
 */
public interface MetricWritePort {
    /**
     * Write a batch of domain metrics to the configured sink.
     */
    void write(List<TrafficMetric> points);
}
