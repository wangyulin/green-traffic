package com.greentraffic.infrastructure.messaging.springevents;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Spring Events 消息发布者
 * 用于开发环境
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "events", matchIfMissing = true)
@RequiredArgsConstructor
public class SpringEventsMessagePublisher implements MessagePublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(Message<?> message) {
        log.debug("Spring Events 发布消息: {}", message.getMessageType());
        applicationEventPublisher.publishEvent(new MessageEvent(this, message));
    }

    @Override
    public void publish(String topic, Message<?> message) {
        message.setTopic(topic);
        publish(message);
    }

    @Override
    public void publishAsync(Message<?> message) {
        // Spring Events 默认是同步的，可以使用 @Async
        publish(message);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
