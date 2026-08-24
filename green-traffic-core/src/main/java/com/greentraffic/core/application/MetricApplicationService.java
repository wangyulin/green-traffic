package com.greentraffic.core.application;

import com.greentraffic.core.port.output.metrics.MetricPoint;
import com.greentraffic.core.port.output.MetricWritePort;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetricApplicationService implements WriteTrafficMetricUseCase {
    private final MetricWritePort writePort;

    public MetricApplicationService(MetricWritePort writePort) {
        this.writePort = writePort;
    }

    public void write(WriteTrafficMetricCommand command) {
        MetricPoint p = toMetricPoint(command);
        writePort.write(List.of(p));
    }

    public void writeBatch(List<WriteTrafficMetricCommand> commands) {
        List<MetricPoint> points = commands.stream().map(this::toMetricPoint).toList();
        writePort.write(points);
    }

    private MetricPoint toMetricPoint(WriteTrafficMetricCommand command) {
        return new MetricPoint(
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
