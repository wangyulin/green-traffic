package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class InMemoryMetricWriteBuffer implements MetricWriteBuffer {
    private final BlockingDeque<TrafficMetric> deque;

    public InMemoryMetricWriteBuffer() {
        this(10000);
    }

    public InMemoryMetricWriteBuffer(int capacity) {
        this.deque = new LinkedBlockingDeque<>(Math.max(1, capacity));
    }

    @Override
    public boolean offer(TrafficMetric point) {
        if (point == null) return true;
        return deque.offer(point);
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
