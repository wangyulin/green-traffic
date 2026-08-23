package com.greentraffic.api.controller.request;

import com.greentraffic.core.port.input.WriteTrafficMetricCommand;

import java.time.Instant;

public record TrafficMetricWriteRequest(
        String roadId,
        String direction,
        String vehicleType,
        Integer trafficFlow,
        Double averageSpeed,
        Double co2Emission,
        String location,
        Instant timestamp
) {
    public WriteTrafficMetricCommand toCommand() {
        return new WriteTrafficMetricCommand(
                roadId, direction, vehicleType, trafficFlow,
                averageSpeed, co2Emission, location, timestamp
        );
    }
}