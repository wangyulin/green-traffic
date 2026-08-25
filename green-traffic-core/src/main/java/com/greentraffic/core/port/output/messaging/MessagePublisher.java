package com.greentraffic.core.port.output.messaging;

import com.greentraffic.core.port.output.messaging.Message;

/**
 * 消息发布者接口
 */
public interface MessagePublisher {

    /**
     * 发布消息
     */
    void publish(Message<?> message);

    /**
     * 发布消息到指定主题
     */
    void publish(String topic, Message<?> message);

    /**
     * 异步发布消息
     */
    void publishAsync(Message<?> message);

    /**
     * 检查是否可用
     */
    boolean isAvailable();
}
