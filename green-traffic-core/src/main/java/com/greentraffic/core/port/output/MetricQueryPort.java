package com.greentraffic.core.port.output;

import com.greentraffic.core.port.output.metrics.MetricPoint;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Port for querying metrics from an external store.
 */
public interface MetricQueryPort {
    /**
     * Query metric points in the given time range with optional tag filters.
     */
    List<MetricPoint> query(Instant from, Instant to, Map<String, String> tags);
}
