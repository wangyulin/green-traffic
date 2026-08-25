package com.greentraffic.infrastructure.messaging.springevents;

import com.greentraffic.core.port.output.messaging.Message;
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
