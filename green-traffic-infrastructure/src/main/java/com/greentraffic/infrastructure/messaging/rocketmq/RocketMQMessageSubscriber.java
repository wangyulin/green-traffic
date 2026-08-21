package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * RocketMQ 消息订阅者
 * 用于测试环境
 */
@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class RocketMQMessageSubscriber implements MessageSubscriber {

    private final RocketMQTemplate rocketMQTemplate;
    private final Map<String, Consumer<Message<?>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void subscribe(String messageType, Consumer<Message<?>> handler) {
        handlers.put(messageType, handler);
        log.info("RocketMQ 订阅消息类型: {}", messageType);
    }

    @Override
    public void subscribeToTopic(String topic, Consumer<Message<?>> handler) {
        handlers.put("topic:" + topic, handler);
        log.info("RocketMQ 订阅主题: {}", topic);
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        handlers.remove(subscriptionId);
    }

    // 使用 RocketMQListener 处理消息
    @Component
    @Profile("test")
    @RocketMQMessageListener(
            topic = "traffic-default-topic",
            consumerGroup = "traffic-consumer-group",
            selectorExpression = "*"
    )
    public class TrafficMessageListener implements RocketMQListener<Object> {

        @Override
        public void onMessage(Object payload) {
            // 构建统一消息
            Message<Object> message = Message.builder()
                    .payload(payload)
                    .messageType("traffic.data")
                    .build();

            // 分发消息
            dispatchMessage(message);
        }
    }

    private void dispatchMessage(Message<?> message) {
        Consumer<Message<?>> handler = handlers.get(message.getMessageType());
        if (handler != null) {
            handler.accept(message);
        }
    }
}
