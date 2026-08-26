package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.converter.SimulationTrafficMessageConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulationTrafficMetricMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(SimulationTrafficMetricMessageConsumer.class);

    private final MessageSubscriber messageSubscriber;

    private final WriteSimulationTrafficMetricUseCase writeUseCase;

    private final SimulationTrafficMessageConverter messageConverter;

    public SimulationTrafficMetricMessageConsumer(
            MessageSubscriber messageSubscriber,
            WriteSimulationTrafficMetricUseCase writeUseCase,
            SimulationTrafficMessageConverter messageConverter) {

        this.messageSubscriber = messageSubscriber;
        this.writeUseCase = writeUseCase;
        this.messageConverter = messageConverter;
    }

    @PostConstruct
    void subscribe() {
        messageSubscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA_BATCH,
                this::consume
        );
    }

    @PreDestroy
    void unsubscribe() {
        messageSubscriber.unsubscribe(
                TrafficMessageTypes.TRAFFIC_DATA_BATCH
        );
    }

    private void consume(Message<?> message) {
        if (message == null) {
            log.warn("Ignoring null simulation traffic message");
            return;
        }

        if (!messageConverter.supports(message)) {
            log.warn(
                    "Ignoring unsupported simulation traffic message type: {}",
                    message.getMessageType()
            );
            return;
        }

        try {
            Message<SimulationTrafficMetric> convertedMessage =
                    messageConverter.convert(message);

            SimulationTrafficMetric metric =
                    convertedMessage.getPayload();

            if (metric == null) {
                log.warn(
                        "Ignoring simulation traffic message with null payload"
                );
                return;
            }

            writeUseCase.write(
                    WriteSimulationTrafficMetricCommand.from(metric)
            );

        } catch (Exception e) {
            log.warn(
                    "Ignoring invalid simulation traffic message: {}",
                    message.getMessageType(),
                    e
            );
        }
    }
}