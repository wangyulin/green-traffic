package com.greentraffic.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.converter.SimulationTrafficMessageConverter;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SimulationTrafficMetricMessageConsumerTest {

    @Test
    void forwardsSimulationMetricsToTheWriteUseCase() {
        MessageSubscriber messageSubscriber =
                mock(MessageSubscriber.class);

        WriteSimulationTrafficMetricUseCase writeSimulationTrafficMetricUseCase =
                mock(WriteSimulationTrafficMetricUseCase.class);

        ObjectMapper objectMapper =
                new ObjectMapper();

        SimulationTrafficMessageConverter converter =
                new SimulationTrafficMessageConverter(objectMapper);

        SimulationTrafficMetricMessageConsumer consumer =
                new SimulationTrafficMetricMessageConsumer(
                        messageSubscriber,
                        writeSimulationTrafficMetricUseCase,
                        converter
                );

        consumer.subscribe();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Message<?>>> handlerCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        verify(messageSubscriber)
                .subscribe(
                        eq(TrafficMessageTypes.TRAFFIC_DATA_BATCH),
                        handlerCaptor.capture()
                );

        SimulationTrafficMetric metric =
                new SimulationTrafficMetric(
                        "sim-1",
                        "SUMO-GRID",
                        "UNKNOWN",
                        "passenger",
                        2,
                        36.0,
                        0.1,
                        15.0,
                        3.0,
                        4.0,
                        400.0,
                        Instant.parse("2026-08-23T08:00:00Z")
                );

        /*
         * 模拟真实 Infrastructure 消息。
         *
         * Payload 是 model.entity.traffic.SimulationTrafficMetric，
         * Consumer 内部通过 SimulationTrafficMessageConverter
         * 转换成 core.domain.traffic.SimulationTrafficMetric。
         */
        Message<SimulationTrafficMetric> message =
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA_BATCH,
                        metric
                );

        handlerCaptor
                .getValue()
                .accept(message);

        ArgumentCaptor<WriteSimulationTrafficMetricCommand>
                commandCaptor =
                ArgumentCaptor.forClass(
                        WriteSimulationTrafficMetricCommand.class
                );

        verify(writeSimulationTrafficMetricUseCase)
                .write(commandCaptor.capture());

        WriteSimulationTrafficMetricCommand command =
                commandCaptor.getValue();

        assertThat(command)
                .isNotNull();

        assertThat(command.simulationId())
                .isEqualTo("sim-1");

        assertThat(command.roadId())
                .isEqualTo("SUMO-GRID");

        assertThat(command.direction())
                .isEqualTo("UNKNOWN");

        assertThat(command.vehicleType())
                .isEqualTo("passenger");

        assertThat(command.vehicleCount())
                .isEqualTo(2);

        assertThat(command.averageSpeed())
                .isEqualTo(36.0);

//        assertThat(command.co2Emission())
//                .isEqualTo(0.1);

        assertThat(command.totalRouteLength())
                .isEqualTo(400.0);
    }
}