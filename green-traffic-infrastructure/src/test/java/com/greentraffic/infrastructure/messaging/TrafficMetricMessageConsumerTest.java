package com.greentraffic.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.infrastructure.messaging.core.MessageSubscriber;
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
        WriteTrafficMetricUseCase writeUseCase = mock(WriteTrafficMetricUseCase.class);
        WriteSimulationTrafficMetricUseCase writeSimulationUseCase =
                mock(WriteSimulationTrafficMetricUseCase.class);
        TrafficMetricMessageConsumer consumer = new TrafficMetricMessageConsumer(
                writeUseCase,
                writeSimulationUseCase
        );

        TrafficMetric metric = new TrafficMetric(
                "ROAD-FAIL", "EAST", "CAR", 1, 30.0, 2.0, null, Instant.now()
        );
        doThrow(new IllegalStateException("storage unavailable"))
                .when(writeUseCase)
                .write(WriteTrafficMetricCommand.from(metric));

        assertThatThrownBy(() -> consumer.consume(
                Message.of(TrafficMessageTypes.TRAFFIC_DATA, WriteTrafficMetricCommand.from(metric))
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("storage unavailable");
    }

    @Test
    void subscribesTrafficMessagesAndForwardsMetricPayloadsToWriteUseCase() {
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
                        writeUseCase,
                        writeSimulationUseCase);

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

        consumer.consume(
                Message.of(
                        TrafficMessageTypes.CO2_EMISSION,
                        WriteTrafficMetricCommand.from(metric)
                )
        );

        verify(writeUseCase).write(WriteTrafficMetricCommand.from(metric));

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

        consumer.consume(
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
        WriteTrafficMetricUseCase writeUseCase2 =
                mock(WriteTrafficMetricUseCase.class);

        WriteSimulationTrafficMetricUseCase writeSimulationUseCase2 =
                mock(WriteSimulationTrafficMetricUseCase.class);

        TrafficMetricMessageConsumer consumer2 =
                new TrafficMetricMessageConsumer(
                        writeUseCase2,
                        writeSimulationUseCase2);

        consumer2.consume(
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        "invalid"
                )
        );

        verify(writeUseCase2, never()).write(any());
        verify(writeSimulationUseCase2, never()).write(any());
    }
}