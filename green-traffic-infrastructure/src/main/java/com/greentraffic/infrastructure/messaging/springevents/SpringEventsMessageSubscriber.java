package com.greentraffic.infrastructure.messaging.springevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.infrastructure.messaging.core.MessageSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.greentraffic.infrastructure.messaging.converter.MessagePayloadConverter;

/**
 * Spring Events 消息订阅者
 * 用于开发环境
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "events", matchIfMissing = true)
public class SpringEventsMessageSubscriber implements MessageSubscriber {

    private final ObjectMapper objectMapper;

    private final Map<String, Consumer<Message<?>>> handlers = new ConcurrentHashMap<>();
    private final List<MessagePayloadConverter<?>> converters;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public SpringEventsMessageSubscriber(ObjectMapper objectMapper, List<MessagePayloadConverter<?>> converters) {
        if (objectMapper == null) {
            this.objectMapper = new ObjectMapper().findAndRegisterModules();
        } else {
            this.objectMapper = objectMapper;
        }
        this.converters = converters;
    }

    @Override
    public void subscribe(String messageType, Consumer<Message<?>> handler) {
        handlers.put(messageType, handler);
        log.info("Spring Events 订阅消息类型: {}", messageType);
    }

    @Override
    public void subscribeToTopic(String topic, Consumer<Message<?>> handler) {
        handlers.put("topic:" + topic, handler);
        log.info("Spring Events 订阅主题: {}", topic);
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        handlers.remove(subscriptionId);
    }

    @EventListener
    public void onMessage(MessageEvent event) {
        try {
            String s = objectMapper.writeValueAsString(event);
            log.info("接收到消息 {}", s);
        } catch (JsonProcessingException e) {
            log.info("接收到消息 (序列化失败) {}", event.toString());
        }
        Message<?> message = event.getMessage();

        Message<?> normalized = convertPayload(message);

        // 根据消息类型分发
        Consumer<Message<?>> typeHandler = handlers.get(normalized.getMessageType());
        if (typeHandler != null) {
            typeHandler.accept(normalized);
        }

        // 根据主题分发
        if (normalized.getTopic() != null) {
            Consumer<Message<?>> topicHandler = handlers.get("topic:" + normalized.getTopic());
            if (topicHandler != null) {
                topicHandler.accept(normalized);
            }
        }
    }

    private Message<?> convertPayload(Message<?> message) {
        if (converters == null || converters.isEmpty()) return message;
        for (MessagePayloadConverter<?> converter : converters) {
            if (converter.supports(message)) {
                return converter.convert(message);
            }
        }
        return message;
    }
}
