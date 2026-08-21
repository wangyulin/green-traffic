package com.greentraffic.infrastructure.router;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 消息路由器
 * 根据环境和消息类型选择合适的消息基础设施
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class MessageRouter implements MessagePublisher {

    private final List<MessagePublisher> publishers;

    @Value("${messaging.type:events}")
    private String activeMessagingType;

    private MessagePublisher getActivePublisher() {
        return publishers.stream()
                .filter(publisher -> publisher.getClass().getSimpleName()
                        .toLowerCase().contains(activeMessagingType.toLowerCase()))
                .filter(MessagePublisher::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "没有可用的消息发布者: " + activeMessagingType));
    }

    @Override
    public void publish(Message<?> message) {
        MessagePublisher publisher = getActivePublisher();
        log.debug("使用 {} 发布消息: {}",
                publisher.getClass().getSimpleName(), message.getMessageType());
        publisher.publish(message);
    }

    @Override
    public void publish(String topic, Message<?> message) {
        getActivePublisher().publish(topic, message);
    }

    @Override
    public void publishAsync(Message<?> message) {
        getActivePublisher().publishAsync(message);
    }

    @Override
    public boolean isAvailable() {
        return getActivePublisher().isAvailable();
    }
}
