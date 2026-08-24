package com.greentraffic.infrastructure.messaging;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessageSubscriber;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class SimulationTrafficMetricMessageConsumer {

    private final MessageSubscriber messageSubscriber;
    private final WriteSimulationTrafficMetricUseCase writeUseCase;

    public SimulationTrafficMetricMessageConsumer(
            MessageSubscriber messageSubscriber,
            WriteSimulationTrafficMetricUseCase writeUseCase) {
        this.messageSubscriber = messageSubscriber;
        this.writeUseCase = writeUseCase;
    }

    @PostConstruct
    void subscribe() {
        messageSubscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA_BATCH, this::consume);
    }

    @PreDestroy
    void unsubscribe() {
        messageSubscriber.unsubscribe(TrafficMessageTypes.TRAFFIC_DATA_BATCH);
    }

    private void consume(Message<?> message) {
        if (message.getPayload() instanceof SimulationTrafficMetric metric) {
            writeUseCase.write(WriteSimulationTrafficMetricCommand.from(metric));
        }
    }
}