package com.greentraffic.infrastructure.events;

import com.greentraffic.common.messaging.Message;
import org.springframework.context.ApplicationEvent;

/**
 * Spring 消息事件
 */
public class MessageEvent extends ApplicationEvent {

    private final Message<?> message;

    public MessageEvent(Object source, Message<?> message) {
        super(source);
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }
}
