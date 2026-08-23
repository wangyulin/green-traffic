package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessageSubscriber;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "messaging.type", havingValue = "rocketmq")
public class RocketMQMessageSubscriber implements MessageSubscriber {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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

    public void dispatchMessage(Message<?> message) {
        Message<?> normalizedMessage = normalizeMetricPayload(message);
        Consumer<Message<?>> handler = handlers.get(normalizedMessage.getMessageType());
        if (handler != null) {
            handler.accept(normalizedMessage);
        }
    }

    private Message<?> normalizeMetricPayload(Message<?> message) {
        if ((TrafficMessageTypes.TRAFFIC_DATA.equals(message.getMessageType())
                || TrafficMessageTypes.CO2_EMISSION.equals(message.getMessageType()))
                && !(message.getPayload() instanceof TrafficMetric)) {
            Message<TrafficMetric> normalized = new Message<>();
            normalized.setMessageId(message.getMessageId());
            normalized.setMessageType(message.getMessageType());
            normalized.setTopic(message.getTopic());
            normalized.setTag(message.getTag());
            normalized.setKey(message.getKey());
            normalized.setHeaders(message.getHeaders());
            normalized.setTimestamp(message.getTimestamp());
            normalized.setPayload(objectMapper.convertValue(message.getPayload(), TrafficMetric.class));
            return normalized;
        }
        return message;
    }
}
