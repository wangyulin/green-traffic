package com.greentraffic.api.controller.dto;

import java.time.Instant;

public record TrafficMetricResponse(
        String roadId,
        String direction,
        String vehicleType,
        Integer trafficFlow,
        Double averageSpeed,
        Double totalCo2Emission,
        String location,
        Instant timestamp
) {
}
