package com.greentraffic.core.port.output.simulation;

/**
 * SUMO tripinfo-output 中单辆车的原始结果。
 */
public record SumoTripInfo(
        String vehicleId,
        String vehicleType,
        double durationSeconds,
        double waitingTimeSeconds,
        double timeLossSeconds,
        double routeLengthMeters
) {
}