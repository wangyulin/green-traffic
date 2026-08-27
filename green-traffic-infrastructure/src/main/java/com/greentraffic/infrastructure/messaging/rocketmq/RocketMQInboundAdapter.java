package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.infrastructure.messaging.TrafficMetricMessageConsumer;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Inbound Adapter: RocketMQ -> Core Input Port
 */
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "rocketmq")
@RocketMQMessageListener(
        topic = "${messaging.rocketmq.topic:traffic-carbon}",
        consumerGroup = "${messaging.rocketmq.consumer-group:green-traffic-vm-consumer}",
        selectorExpression = "*"
)
public class RocketMQInboundAdapter implements RocketMQListener<Message<?>> {

    private final TrafficMetricMessageConsumer handler;

    public RocketMQInboundAdapter(
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase) {
        this.handler = new TrafficMetricMessageConsumer(writeUseCase, writeSimulationUseCase);
    }

    @Override
    public void onMessage(Message<?> message) {
        handler.consume(message);
    }
}
