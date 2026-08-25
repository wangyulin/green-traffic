package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrafficMetricMessageConsumerTest {

    @Test
    void subscribesTrafficMessagesAndForwardsMetricPayloadsToWriteUseCase() {
        MessageSubscriber messageSubscriber = mock(MessageSubscriber.class);
        WriteTrafficMetricUseCase writeUseCase = mock(WriteTrafficMetricUseCase.class);
        com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase writeSimulationUseCase = mock(com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase.class);
        TrafficMetricMessageConsumer consumer = new TrafficMetricMessageConsumer(messageSubscriber, writeUseCase, writeSimulationUseCase);

        consumer.subscribe();

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageSubscriber, org.mockito.Mockito.times(2)).subscribe(typeCaptor.capture(), handlerCaptor.capture());
        Map<String, Consumer<Message<?>>> handlers = new HashMap<>();
        for (int index = 0; index < typeCaptor.getAllValues().size(); index++) {
            handlers.put(typeCaptor.getAllValues().get(index), handlerCaptor.getAllValues().get(index));
        }

        TrafficMetric metric = new TrafficMetric(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, null,
                Instant.parse("2026-08-22T10:00:00Z")
        );
        handlers.get(TrafficMessageTypes.CO2_EMISSION)
                .accept(Message.of(TrafficMessageTypes.CO2_EMISSION, metric));

        verify(writeUseCase).write(WriteTrafficMetricCommand.from(metric));
        assertThat(handlers).containsKeys(
                TrafficMessageTypes.TRAFFIC_DATA,
                TrafficMessageTypes.CO2_EMISSION
        );

        // also ensure SimulationTrafficMetric payloads on CO2_EMISSION are forwarded
        com.greentraffic.model.entity.traffic.SimulationTrafficMetric sim = new com.greentraffic.model.entity.traffic.SimulationTrafficMetric(
            "sim-1", "ROAD-001", "EAST", "CAR", 10, 40.0, 0.5, 5.0, 0.5, 0.2, 15000.0, Instant.parse("2026-08-22T10:00:00Z")
        );
        handlers.get(TrafficMessageTypes.CO2_EMISSION)
            .accept(Message.of(TrafficMessageTypes.CO2_EMISSION, sim));

        verify(writeSimulationUseCase).write(com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand.from(sim));
    }

    @Test
    void ignoresNonMetricPayloads() {
        MessageSubscriber messageSubscriber = mock(MessageSubscriber.class);
        WriteTrafficMetricUseCase writeUseCase = mock(WriteTrafficMetricUseCase.class);
        com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase writeSimulationUseCase = mock(com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase.class);
        TrafficMetricMessageConsumer consumer = new TrafficMetricMessageConsumer(messageSubscriber, writeUseCase, writeSimulationUseCase);

        consumer.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(messageSubscriber, org.mockito.Mockito.times(2))
                .subscribe(org.mockito.ArgumentMatchers.anyString(), handlerCaptor.capture());
        handlerCaptor.getAllValues().get(0).accept(Message.of(TrafficMessageTypes.TRAFFIC_DATA, "invalid"));

        verify(writeUseCase, org.mockito.Mockito.never()).write(org.mockito.ArgumentMatchers.any());
    }
}