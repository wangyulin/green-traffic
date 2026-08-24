package com.greentraffic.core.port.output.simulation;

import java.nio.file.Path;

public record SumoSimulationRequest(
        String simulationId,
        Path workingDirectory,
        int durationSeconds,
        int vehiclesPerHour
) {
}