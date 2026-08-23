package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.common.messaging.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "rocketmq")
@RocketMQMessageListener(
        topic = "${messaging.rocketmq.topic:traffic-carbon}",
        consumerGroup = "${messaging.rocketmq.consumer-group:green-traffic-vm-consumer}",
        selectorExpression = "*"
)
public class RocketMQTrafficMessageListener implements RocketMQListener<Message<?>> {

    private final RocketMQMessageSubscriber subscriber;

    public RocketMQTrafficMessageListener(RocketMQMessageSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onMessage(Message<?> message) {
        subscriber.dispatchMessage(message);
    }
}