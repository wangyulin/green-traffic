package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.infrastructure.messaging.TrafficMetricMessageConsumer;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
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
    private final com.greentraffic.infrastructure.messaging.rocketmq.consumer.RocketMQMessageSubscriber subscriber;

    public RocketMQInboundAdapter(
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase,
            com.greentraffic.infrastructure.messaging.rocketmq.consumer.RocketMQMessageSubscriber subscriber) {
        this.handler = new TrafficMetricMessageConsumer(writeUseCase, writeSimulationUseCase);
        this.subscriber = subscriber;
        // 注册默认的消息类型处理器，确保 subscriber 能将消息分发给 handler
        if (this.subscriber != null) {
            this.subscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA_BATCH, this.handler::consume);
            this.subscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA, this.handler::consume);
            this.subscriber.subscribe(TrafficMessageTypes.CO2_EMISSION, this.handler::consume);
        }
    }

    @Override
    public void onMessage(Message<?> message) {
        // let the RocketMQ subscriber normalize payload via converters and dispatch to registered handlers
        if (subscriber != null) {
            subscriber.dispatchMessage(message);
            return;
        }

        // fallback: normalize minimally via TrafficMetricMessageConsumer
        handler.consume(message);
    }
}
