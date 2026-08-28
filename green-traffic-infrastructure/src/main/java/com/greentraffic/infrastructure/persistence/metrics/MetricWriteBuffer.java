package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import java.util.List;

public interface MetricWriteBuffer {
    /**
     * Offer a point to the buffer.
     * @param point point to add
     * @return true if accepted, false if buffer is full and point was not enqueued
     */
    boolean offer(TrafficMetric point);
    List<TrafficMetric> drain(int maxBatchSize);
    int size();
    void requeueAtFront(List<TrafficMetric> points);
}
