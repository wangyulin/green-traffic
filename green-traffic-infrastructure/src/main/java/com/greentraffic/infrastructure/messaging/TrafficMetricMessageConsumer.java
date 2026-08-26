package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TrafficMetricMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TrafficMetricMessageConsumer.class);

    private final MessageSubscriber messageSubscriber;

    private final WriteTrafficMetricUseCase writeUseCase;
    private final WriteSimulationTrafficMetricUseCase writeSimulationUseCase;

    public TrafficMetricMessageConsumer(
            MessageSubscriber messageSubscriber,
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase) {

        this.messageSubscriber = messageSubscriber;
        this.writeUseCase = writeUseCase;
        this.writeSimulationUseCase = writeSimulationUseCase;
    }

    @PostConstruct
    void subscribe() {
        messageSubscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA,
                this::consume
        );

        messageSubscriber.subscribe(
                TrafficMessageTypes.CO2_EMISSION,
                this::consume
        );
    }

    @PreDestroy
    void unsubscribe() {
        messageSubscriber.unsubscribe(
                TrafficMessageTypes.TRAFFIC_DATA
        );

        messageSubscriber.unsubscribe(
                TrafficMessageTypes.CO2_EMISSION
        );
    }

    private void consume(Message<?> message) {
        if (message == null) {
            log.warn("Ignoring null traffic metric message");
            return;
        }

        Object payload = message.getPayload();

        if (payload instanceof TrafficMetric metric) {
            try {
                writeUseCase.write(
                        WriteTrafficMetricCommand.from(metric)
                );
            } catch (Exception e) {
                log.warn(
                        "Failed to process {} message",
                        message.getMessageType(),
                        e
                );
            }
            return;
        }

        // 支持来自 model 层的仿真实体，也支持已经被转换为 core.domain 的仿真实体
        if (payload instanceof SimulationTrafficMetric simModel) {
            try {
                com.greentraffic.core.domain.traffic.SimulationTrafficMetric domainSim =
                        new com.greentraffic.core.domain.traffic.SimulationTrafficMetric(
                                simModel.simulationId(),
                                simModel.roadId(),
                                simModel.direction(),
                                simModel.vehicleType(),
                                simModel.vehicleCount(),
                                simModel.averageSpeed(),
                                simModel.totalCo2Emission(),
                                simModel.averageTravelTime(),
                                simModel.averageWaitingTime(),
                                simModel.averageTimeLoss(),
                                simModel.totalRouteLength(),
                                simModel.timestamp()
                        );

                writeSimulationUseCase.write(
                        WriteSimulationTrafficMetricCommand.from(domainSim)
                );
            } catch (Exception e) {
                log.warn(
                        "Failed to process {} message",
                        message.getMessageType(),
                        e
                );
            }

            return;
        }

        if (payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric domainSim) {
            try {
                writeSimulationUseCase.write(
                        WriteSimulationTrafficMetricCommand.from(domainSim)
                );
            } catch (Exception e) {
                log.warn(
                        "Failed to process {} message",
                        message.getMessageType(),
                        e
                );
            }

            return;
        }

        log.warn(
                "Ignoring {} message with unsupported payload type: {}",
                message.getMessageType(),
                payload == null ? "null" : payload.getClass().getName()
        );
    }
}