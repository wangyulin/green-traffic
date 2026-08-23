package com.greentraffic.core.application;

import com.greentraffic.common.port.metrics.MetricPoint;
import com.greentraffic.common.port.metrics.MetricQueryPort;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TrafficMetricQueryApplicationService implements QueryTrafficMetricUseCase {

    private final MetricQueryPort queryPort;

    public TrafficMetricQueryApplicationService(MetricQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<TrafficMetric> query(Instant start, Instant end) {
        return queryPort.query(start, end, java.util.Map.of()).stream()
                .map(this::toTrafficMetric)
                .toList();
    }

    private TrafficMetric toTrafficMetric(MetricPoint point) {
        return new TrafficMetric(
                point.roadId(),
                point.direction(),
                point.vehicleType(),
                point.trafficFlow(),
                point.averageSpeed(),
                point.co2Emission(),
                point.location(),
                point.timestamp()
        );
    }
}