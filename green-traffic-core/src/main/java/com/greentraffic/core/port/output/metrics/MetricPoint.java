package com.greentraffic.core.port.output.metrics;

import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight DTO for metrics used by Ports/Adapters.
 */
public record MetricPoint(
        String roadId,
        String direction,
        String vehicleType,
        Integer trafficFlow,
        Double averageSpeed,
        Double co2Emission,
        String location,
        Instant timestamp
) {
    public MetricPoint {
        // defensive defaults could be added here if needed
    }

    @Override
    public String toString() {
        return "MetricPoint{" +
                "roadId='" + roadId + '\'' +
                ", direction='" + direction + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", trafficFlow=" + trafficFlow +
                ", averageSpeed=" + averageSpeed +
                ", co2Emission=" + co2Emission +
                ", location='" + location + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
