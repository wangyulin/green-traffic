package com.greentraffic.core.application;

import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;

import java.time.Instant;
import java.util.List;

public class TrafficMetricQueryApplicationService implements QueryTrafficMetricUseCase {

    private final TrafficMetricStore queryPort;

    public TrafficMetricQueryApplicationService(TrafficMetricStore queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<TrafficMetric> query(Instant start, Instant end) {

        TrafficMetricQuery query = new TrafficMetricQuery(start, end);
        return queryPort.query(query);
    }

}