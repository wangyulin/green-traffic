package com.greentraffic.simulator;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.output.simulation.SimulationEnginePort;
import com.greentraffic.core.port.output.simulation.SumoTripInfo;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import com.greentraffic.simulator.sumo.SumoSimulatorProperties;
import com.greentraffic.simulator.sumo.SumoTrafficSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SumoTrafficSimulatorTest {

    @Test
    void publishesAggregatedSumaResultThroughTheMessagePort() {
        SimulationEnginePort sumoPort = mock(SimulationEnginePort.class);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(sumoPort.run(any())).thenReturn(List.of(
                new SumoTripInfo("car-1", "passenger", 10, 2, 3, 100),
                new SumoTripInfo("car-2", "passenger", 20, 4, 5, 300)));
        SumoSimulatorProperties properties = new SumoSimulatorProperties();
        properties.setWorkingDirectory(Path.of("build/test-sumo"));
        properties.setDurationSeconds(60);
        properties.setVehiclesPerHour(120);
        SumoTrafficSimulator simulator = new SumoTrafficSimulator(sumoPort, messagePublisher, properties, objectMapper);

        simulator.simulateAndPublish();

        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagePublisher).publishAsync(captor.capture());
        assertThat(captor.getValue().getMessageType()).isEqualTo(TrafficMessageTypes.TRAFFIC_DATA_BATCH);
        SimulationTrafficMetric metric = (SimulationTrafficMetric) captor.getValue().getPayload();
        assertThat(metric.vehicleCount()).isEqualTo(2);
        assertThat(metric.averageSpeed()).isEqualTo(45.0);
        assertThat(metric.averageTravelTime()).isEqualTo(15.0);
        assertThat(metric.averageWaitingTime()).isEqualTo(3.0);
        assertThat(metric.totalRouteLength()).isEqualTo(400.0);
    }
}