package com.greentraffic.core.port.input;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;

import java.time.Instant;

public record WriteSimulationTrafficMetricCommand(
        String simulationId,
        String roadId,
        String direction,
        String vehicleType,
        Integer vehicleCount,
        Double averageSpeed,
        Double totalCo2Emission,
        Double averageTravelTime,
        Double averageWaitingTime,
        Double averageTimeLoss,
        Double totalRouteLength,
        Instant timestamp
) {
    public static WriteSimulationTrafficMetricCommand from(SimulationTrafficMetric metric) {
        return new WriteSimulationTrafficMetricCommand(
                metric.simulationId(),
                metric.roadId(),
                metric.direction(),
                metric.vehicleType(),
                metric.vehicleCount(),
                metric.averageSpeed(),
                metric.totalCo2Emission(),
                metric.averageTravelTime(),
                metric.averageWaitingTime(),
                metric.averageTimeLoss(),
                metric.totalRouteLength(),
                metric.timestamp()
        );
    }
}