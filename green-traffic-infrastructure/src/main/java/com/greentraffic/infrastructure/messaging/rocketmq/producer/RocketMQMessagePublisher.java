package com.greentraffic.infrastructure.messaging.rocketmq.producer;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * RocketMQ 消息发布者
 * 用于测试环境
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "rocketmq")
@RequiredArgsConstructor
public class RocketMQMessagePublisher implements MessagePublisher {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${messaging.rocketmq.topic:traffic-carbon}")
    private String defaultTopic;

    @Override
    public void publish(Message<?> message) {
        String destination = buildDestination(message);

        try {
            SendResult result = rocketMQTemplate.syncSend(
                    destination,
                    buildSpringMessage(message)
            );
            log.info("RocketMQ 发送消息成功: {}, msgId: {}",
                    message.getMessageType(), result.getMsgId());
        } catch (Exception e) {
            log.error("RocketMQ 发送消息失败", e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    @Override
    public void publish(String topic, Message<?> message) {
        message.setTopic(topic);
        publish(message);
    }

    @Override
    public void publishAsync(Message<?> message) {
        String destination = buildDestination(message);

        rocketMQTemplate.asyncSend(destination, buildSpringMessage(message),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("RocketMQ 异步发送成功: {}", sendResult.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("RocketMQ 异步发送失败", e);
                    }
                });
    }

    @Override
    public boolean isAvailable() {
        try {
            rocketMQTemplate.getProducer();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildDestination(Message<?> message) {
        String topic = message.getTopic() != null ?
            message.getTopic() : defaultTopic;

        String tag = message.getTag() != null ?
            message.getTag() : message.getMessageType();

        return topic + ":" + tag;
    }

    private org.springframework.messaging.Message<?> buildSpringMessage(Message<?> message) {
        return MessageBuilder
            .withPayload(message)
                .build();
    }
}
