package com.greentraffic.core.application;

import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;

import java.util.List;

public class MetricApplicationService implements WriteTrafficMetricUseCase {
    private final TrafficMetricStore writePort;

    public MetricApplicationService(TrafficMetricStore writePort) {
        this.writePort = writePort;
    }

    public void write(WriteTrafficMetricCommand command) {
        writePort.write(List.of(commandToDomain(command)));
    }

    public void writeBatch(List<WriteTrafficMetricCommand> commands) {
        List<com.greentraffic.core.domain.traffic.TrafficMetric> points =
                commands.stream().map(this::commandToDomain).toList();
        writePort.write(points);
    }

    private com.greentraffic.core.domain.traffic.TrafficMetric commandToDomain(WriteTrafficMetricCommand command) {
        return new com.greentraffic.core.domain.traffic.TrafficMetric(
                command.roadId(),
                command.direction(),
                command.vehicleType(),
                command.trafficFlow(),
                command.averageSpeed(),
                command.co2Emission(),
                command.location(),
                command.timestamp()
        );
    }
}
