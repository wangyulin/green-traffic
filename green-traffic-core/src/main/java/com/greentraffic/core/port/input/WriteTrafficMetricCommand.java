package com.greentraffic.core.port.input;

import com.greentraffic.core.domain.traffic.TrafficMetric;

import java.time.Instant;

public record WriteTrafficMetricCommand(
        String roadId,
        String direction,
        String vehicleType,
        Integer trafficFlow,
        Double averageSpeed,
        Double co2Emission,
        String location,
        Instant timestamp
) {
    public static WriteTrafficMetricCommand from(TrafficMetric metric) {
        return new WriteTrafficMetricCommand(
                metric.roadId(),
                metric.direction(),
                metric.vehicleType(),
                metric.trafficFlow(),
                metric.averageSpeed(),
                metric.co2Emission(),
                metric.location(),
                metric.timestamp()
        );
    }
}