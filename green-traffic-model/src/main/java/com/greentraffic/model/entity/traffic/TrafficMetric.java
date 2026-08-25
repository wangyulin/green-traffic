package com.greentraffic.model.entity.traffic;

import java.time.Instant;

public record TrafficMetric(
        String roadId,
        String direction,
        String vehicleType,
        Integer trafficFlow,
        Double averageSpeed,
        Double co2Emission,
        String location,
        Instant timestamp
) {
}
