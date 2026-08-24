package com.greentraffic.core.application;

import com.greentraffic.core.port.output.metrics.SimulationMetricPoint;
import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationMetricApplicationService implements WriteSimulationTrafficMetricUseCase {

    private final SimulationMetricWritePort writePort;

    public SimulationMetricApplicationService(SimulationMetricWritePort writePort) {
        this.writePort = writePort;
    }

    @Override
    public void write(WriteSimulationTrafficMetricCommand command) {
        writePort.write(List.of(new SimulationMetricPoint(
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