package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SimulationTrafficMetricMessageConsumerTest {

    @Test
    void forwardsSimulationMetricsToTheWriteUseCase() {
        MessageSubscriber subscriber = mock(MessageSubscriber.class);
        WriteSimulationTrafficMetricUseCase writeUseCase = mock(WriteSimulationTrafficMetricUseCase.class);
        SimulationTrafficMetricMessageConsumer consumer = new SimulationTrafficMetricMessageConsumer(subscriber, writeUseCase);

        consumer.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(subscriber).subscribe(org.mockito.ArgumentMatchers.eq(TrafficMessageTypes.TRAFFIC_DATA_BATCH), handlerCaptor.capture());
        SimulationTrafficMetric metric = new SimulationTrafficMetric(
                "sim-1", "SUMO-GRID", "UNKNOWN", "passenger", 2, 36.0,
                0.1, 15.0, 3.0, 4.0, 400.0, Instant.parse("2026-08-23T08:00:00Z"));

        handlerCaptor.getValue().accept(Message.of(TrafficMessageTypes.TRAFFIC_DATA_BATCH, metric));

        ArgumentCaptor<WriteSimulationTrafficMetricCommand> commandCaptor = ArgumentCaptor.forClass(WriteSimulationTrafficMetricCommand.class);
        verify(writeUseCase).write(commandCaptor.capture());
        assertThat(commandCaptor.getValue().simulationId()).isEqualTo("sim-1");
        assertThat(commandCaptor.getValue().totalRouteLength()).isEqualTo(400.0);
    }
}