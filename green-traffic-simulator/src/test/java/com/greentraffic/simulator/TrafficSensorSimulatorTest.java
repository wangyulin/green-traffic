package com.greentraffic.simulator;

import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.simulator.scheduling.TrafficSensorSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrafficSensorSimulatorTest {

    @Test
    void submitsGeneratedMetricThroughWriteUseCase() {
        WriteTrafficMetricUseCase writeUseCase = mock(WriteTrafficMetricUseCase.class);
        TrafficSensorSimulator simulator = new TrafficSensorSimulator(writeUseCase);

        simulator.generateTrafficData();

        ArgumentCaptor<WriteTrafficMetricCommand> commandCaptor = ArgumentCaptor.forClass(WriteTrafficMetricCommand.class);
        verify(writeUseCase).write(commandCaptor.capture());
        WriteTrafficMetricCommand command = commandCaptor.getValue();
        assertThat(command.roadId()).isEqualTo("ROAD-001");
        assertThat(command.direction()).isEqualTo("EAST");
        assertThat(command.trafficFlow()).isBetween(100, 140);
        assertThat(command.averageSpeed()).isBetween(37.5, 47.5);
        assertThat(command.timestamp()).isNotNull();
    }
}