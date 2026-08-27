package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import java.util.List;

public interface MetricWriteBuffer {
    void offer(TrafficMetric point);
    List<TrafficMetric> drain(int maxBatchSize);
    int size();
    void requeueAtFront(List<TrafficMetric> points);
}
