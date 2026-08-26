package com.greentraffic.core.domain.traffic;

import java.time.Instant;

/**
 * SUMO 单次仿真窗口聚合后的交通指标。
 */
public record SimulationTrafficMetric(
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
