package com.greentraffic.common.messaging;

import java.util.function.Consumer;

/**
 * 消息订阅者接口
 */
public interface MessageSubscriber {

    /**
     * 订阅消息
     */
    void subscribe(String messageType, Consumer<Message<?>> handler);

    /**
     * 订阅主题
     */
    void subscribeToTopic(String topic, Consumer<Message<?>> handler);

    /**
     * 取消订阅
     */
    void unsubscribe(String subscriptionId);
}
