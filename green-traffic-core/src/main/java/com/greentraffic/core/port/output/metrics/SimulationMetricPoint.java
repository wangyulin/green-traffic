package com.greentraffic.core.port.output.metrics;

import java.time.Instant;

/**
 * 供时序存储适配器写入的 SUMO 仿真指标。
 */
public record SimulationMetricPoint(
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
}