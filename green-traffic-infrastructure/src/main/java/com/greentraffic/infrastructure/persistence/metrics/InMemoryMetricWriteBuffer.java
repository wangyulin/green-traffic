package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class InMemoryMetricWriteBuffer implements MetricWriteBuffer {
    private final BlockingDeque<TrafficMetric> deque = new LinkedBlockingDeque<>();

    @Override
    public void offer(TrafficMetric point) {
        if (point == null) return;
        deque.offer(point);
    }

    @Override
    public List<TrafficMetric> drain(int maxBatchSize) {
        List<TrafficMetric> drained = new ArrayList<>(Math.max(0, maxBatchSize));
        deque.drainTo(drained, Math.max(0, maxBatchSize));
        return drained;
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public void requeueAtFront(List<TrafficMetric> points) {
        if (points == null) return;
        for (int i = points.size() - 1; i >= 0; i--) {
            deque.offerFirst(points.get(i));
        }
    }
}
