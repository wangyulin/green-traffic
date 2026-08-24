package com.greentraffic.core.port.output;

import com.greentraffic.core.port.output.metrics.MetricPoint;

import java.util.List;

/**
 * Port for writing metrics to an external sink (InfluxDB, VictoriaMetrics, ...)
 */
public interface MetricWritePort {
    /**
     * Write a batch of metric points to the configured sink.
     * Implementations should handle batching/retries as needed.
     */
    void write(List<MetricPoint> points);
}
