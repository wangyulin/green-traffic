package com.greentraffic.core.application;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;

import java.util.List;

public class SimulationMetricApplicationService implements WriteSimulationTrafficMetricUseCase {

    private final SimulationMetricStore writePort;

    public SimulationMetricApplicationService(SimulationMetricStore writePort) {
        this.writePort = writePort;
    }

    @Override
    public void write(WriteSimulationTrafficMetricCommand command) {
        writePort.write(List.of(new SimulationTrafficMetric(
            command.simulationId(),
            command.roadId(),
            command.direction(),
            command.vehicleType(),
            command.vehicleCount(),
            command.averageSpeed(),
            command.totalCo2Emission(),
            command.averageTravelTime(),
            command.averageWaitingTime(),
            command.averageTimeLoss(),
            command.totalRouteLength(),
            command.timestamp()
        )));
    }
}