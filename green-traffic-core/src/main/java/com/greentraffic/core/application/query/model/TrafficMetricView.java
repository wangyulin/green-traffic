package com.greentraffic.core.application.query.model;

import java.time.Instant;

public record TrafficMetricView(
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
