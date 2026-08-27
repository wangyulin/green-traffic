package com.greentraffic.infrastructure.messaging.core;

import java.util.function.Consumer;

/**
 * Infrastructure 层的消息订阅者接口（Inbound Adapter 方向）
 */
public interface MessageSubscriber {

    /**
     * 订阅消息类型
     */
    void subscribe(String messageType, Consumer<com.greentraffic.core.port.output.messaging.Message<?>> handler);

    /**
     * 订阅主题
     */
    void subscribeToTopic(String topic, Consumer<com.greentraffic.core.port.output.messaging.Message<?>> handler);

    /**
     * 取消订阅
     */
    void unsubscribe(String subscriptionId);
}
