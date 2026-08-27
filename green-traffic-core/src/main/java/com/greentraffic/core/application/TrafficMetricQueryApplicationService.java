package com.greentraffic.core.application;

import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;
import com.greentraffic.core.application.query.model.TrafficMetricView;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TrafficMetricQueryApplicationService implements QueryTrafficMetricUseCase {

    private final TrafficMetricStore queryPort;

    public TrafficMetricQueryApplicationService(TrafficMetricStore queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<TrafficMetricView> query(Instant start, Instant end) {

        TrafficMetricQuery query = new TrafficMetricQuery(start, end);
        List<TrafficMetric> result = queryPort.query(query);

        return result.stream().map(this::toView).collect(Collectors.toList());
    }

    private TrafficMetricView toView(TrafficMetric m) {
        return new TrafficMetricView(
                m.roadId(),
                m.direction(),
                m.vehicleType(),
                m.trafficFlow(),
                m.averageSpeed(),
                m.co2Emission(),
                m.location(),
                m.timestamp()
        );
    }

}