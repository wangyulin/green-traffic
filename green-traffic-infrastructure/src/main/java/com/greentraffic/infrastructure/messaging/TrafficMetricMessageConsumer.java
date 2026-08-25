package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TrafficMetricMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficMetricMessageConsumer.class);

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
        messageSubscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA, this::consume);
        messageSubscriber.subscribe(TrafficMessageTypes.CO2_EMISSION, this::consume);
    }

    @PreDestroy
    void unsubscribe() {
        messageSubscriber.unsubscribe(TrafficMessageTypes.TRAFFIC_DATA);
        messageSubscriber.unsubscribe(TrafficMessageTypes.CO2_EMISSION);
    }

    private void consume(Message<?> message) {
        if (message.getPayload() instanceof TrafficMetric metric) {
            writeUseCase.write(WriteTrafficMetricCommand.from(metric));
            return;
        }

        if (message.getPayload() instanceof SimulationTrafficMetric simMetric) {
            writeSimulationUseCase.write(WriteSimulationTrafficMetricCommand.from(simMetric));
            return;
        }

        log.warn("Ignoring {} message with unsupported payload type: {}",
                message.getMessageType(),
                message.getPayload() == null ? "null" : message.getPayload().getClass().getName());
    }
}