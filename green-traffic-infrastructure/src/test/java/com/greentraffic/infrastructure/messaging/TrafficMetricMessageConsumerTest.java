package com.greentraffic.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.converter.SimulationTrafficMessageConverter;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TrafficMetricMessageConsumerTest {

    @Test
    void propagatesWriteFailureSoMessageBrokerCanRetry() {
        MessageSubscriber messageSubscriber = mock(MessageSubscriber.class);
        WriteTrafficMetricUseCase writeUseCase = mock(WriteTrafficMetricUseCase.class);
        WriteSimulationTrafficMetricUseCase writeSimulationUseCase =
                mock(WriteSimulationTrafficMetricUseCase.class);
        TrafficMetricMessageConsumer consumer = new TrafficMetricMessageConsumer(
                messageSubscriber,
                writeUseCase,
                writeSimulationUseCase
        );

        consumer.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(messageSubscriber, times(2)).subscribe(any(), handlerCaptor.capture());

        TrafficMetric metric = new TrafficMetric(
                "ROAD-FAIL", "EAST", "CAR", 1, 30.0, 2.0, null, Instant.now()
        );
        doThrow(new IllegalStateException("storage unavailable"))
                .when(writeUseCase)
                .write(WriteTrafficMetricCommand.from(metric));

        assertThatThrownBy(() -> handlerCaptor.getAllValues().get(0).accept(
                Message.of(TrafficMessageTypes.TRAFFIC_DATA, WriteTrafficMetricCommand.from(metric))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("storage unavailable");
    }

    @Test
    void subscribesTrafficMessagesAndForwardsMetricPayloadsToWriteUseCase() {
        MessageSubscriber messageSubscriber =
                mock(MessageSubscriber.class);

        WriteTrafficMetricUseCase writeUseCase =
                mock(WriteTrafficMetricUseCase.class);

        WriteSimulationTrafficMetricUseCase writeSimulationUseCase =
                mock(WriteSimulationTrafficMetricUseCase.class);

        ObjectMapper objectMapper =
                new ObjectMapper();

        SimulationTrafficMessageConverter simulationTrafficMessageConverter =
                new SimulationTrafficMessageConverter(objectMapper);

        TrafficMetricMessageConsumer consumer =
                new TrafficMetricMessageConsumer(
                        messageSubscriber,
                        writeUseCase,
                        writeSimulationUseCase);

        consumer.subscribe();

        ArgumentCaptor<String> typeCaptor =
                ArgumentCaptor.forClass(String.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        verify(messageSubscriber, times(2))
                .subscribe(
                        typeCaptor.capture(),
                        handlerCaptor.capture()
                );

        Map<String, Consumer<Message<?>>> handlers =
                new HashMap<>();

        for (int index = 0;
             index < typeCaptor.getAllValues().size();
             index++) {

            handlers.put(
                    typeCaptor.getAllValues().get(index),
                    handlerCaptor.getAllValues().get(index)
            );
        }

        assertThat(handlers)
                .containsKeys(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        TrafficMessageTypes.CO2_EMISSION
                );

        /*
         * 普通 TrafficMetric
         */
        TrafficMetric metric =
                new TrafficMetric(
                        "ROAD-001",
                        "EAST",
                        "CAR",
                        120,
                        42.5,
                        12.3,
                        null,
                        Instant.parse("2026-08-22T10:00:00Z")
                );

        handlers.get(TrafficMessageTypes.CO2_EMISSION)
                .accept(
                        Message.of(
                                TrafficMessageTypes.CO2_EMISSION,
                                WriteTrafficMetricCommand.from(metric)
                        )
                );

        verify(writeUseCase)
                .write(
                        WriteTrafficMetricCommand.from(metric)
                );

        /*
         * SimulationTrafficMetric
         *
         * 这里故意使用 model entity，
         * 因为这是 Infrastructure 层收到消息时可能存在的 Payload 类型。
         *
         * Consumer / Converter 负责将其转换为
         * core.domain.traffic.SimulationTrafficMetric。
         */
        SimulationTrafficMetric simulationMetric =
                new SimulationTrafficMetric(
                        "sim-1",
                        "ROAD-001",
                        "EAST",
                        "CAR",
                        10,
                        40.0,
                        0.5,
                        5.0,
                        0.5,
                        0.2,
                        15000.0,
                        Instant.parse("2026-08-22T10:00:00Z")
                );

        handlers.get(TrafficMessageTypes.CO2_EMISSION)
                .accept(
                        Message.of(
                                TrafficMessageTypes.CO2_EMISSION,
                                WriteSimulationTrafficMetricCommand.from(simulationMetric)
                        )
                );

        ArgumentCaptor<WriteSimulationTrafficMetricCommand>
                simulationCommandCaptor =
                ArgumentCaptor.forClass(
                        WriteSimulationTrafficMetricCommand.class
                );

        verify(writeSimulationUseCase)
                .write(simulationCommandCaptor.capture());

        WriteSimulationTrafficMetricCommand simulationCommand =
                simulationCommandCaptor.getValue();

        assertThat(simulationCommand)
                .isNotNull();

        assertThat(simulationCommand.simulationId())
                .isEqualTo("sim-1");

        assertThat(simulationCommand.roadId())
                .isEqualTo("ROAD-001");

        assertThat(simulationCommand.direction())
                .isEqualTo("EAST");

        assertThat(simulationCommand.vehicleType())
                .isEqualTo("CAR");

        assertThat(simulationCommand.totalRouteLength())
                .isEqualTo(15000.0);
    }

    @Test
    void ignoresNonMetricPayloads() {
        MessageSubscriber messageSubscriber =
                mock(MessageSubscriber.class);

        WriteTrafficMetricUseCase writeUseCase =
                mock(WriteTrafficMetricUseCase.class);

        WriteSimulationTrafficMetricUseCase writeSimulationUseCase =
                mock(WriteSimulationTrafficMetricUseCase.class);

        ObjectMapper objectMapper =
                new ObjectMapper();

        SimulationTrafficMessageConverter simulationTrafficMessageConverter =
                new SimulationTrafficMessageConverter(objectMapper);

        TrafficMetricMessageConsumer consumer =
                new TrafficMetricMessageConsumer(
                        messageSubscriber,
                        writeUseCase,
                        writeSimulationUseCase);

        consumer.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        verify(messageSubscriber, times(2))
                .subscribe(
                        org.mockito.ArgumentMatchers.anyString(),
                        handlerCaptor.capture()
                );

        Consumer<Message<?>> handler =
                handlerCaptor.getAllValues().get(0);

        handler.accept(
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        "invalid"
                )
        );

        verify(writeUseCase, never())
                .write(any());

        verify(writeSimulationUseCase, never())
                .write(any());
    }
}