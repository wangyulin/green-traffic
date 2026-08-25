package com.greentraffic.infrastructure.messaging.rocketmq.consumer;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.infrastructure.messaging.converter.MessagePayloadConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * RocketMQ 消息订阅者
 * 用于测试环境
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "messaging.type",
        havingValue = "rocketmq"
)
public class RocketMQMessageSubscriber implements MessageSubscriber {

    private final Map<String, Consumer<Message<?>>> handlers =
            new ConcurrentHashMap<>();

    private final List<MessagePayloadConverter<?>> converters;

    public RocketMQMessageSubscriber(
            List<MessagePayloadConverter<?>> converters) {

        this.converters = converters;
    }

    @Override
    public void subscribe(
            String messageType,
            Consumer<Message<?>> handler) {

        handlers.put(messageType, handler);

        log.info(
                "RocketMQ 订阅消息类型: {}",
                messageType
        );
    }

    @Override
    public void subscribeToTopic(
            String topic,
            Consumer<Message<?>> handler) {

        handlers.put(
                "topic:" + topic,
                handler
        );

        log.info(
                "RocketMQ 订阅主题: {}",
                topic
        );
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        handlers.remove(subscriptionId);
    }

    public void dispatchMessage(Message<?> message) {

        Message<?> normalizedMessage =
                convertPayload(message);

        Consumer<Message<?>> handler =
                handlers.get(
                        normalizedMessage.getMessageType()
                );

        if (handler != null) {
            handler.accept(normalizedMessage);
            return;
        }

        log.debug(
                "No handler registered for message type: {}",
                normalizedMessage.getMessageType()
        );
    }

    private Message<?> convertPayload(Message<?> message) {

        for (MessagePayloadConverter<?> converter : converters) {

            if (converter.supports(message)) {
                return converter.convert(message);
            }
        }

        return message;
    }
}
