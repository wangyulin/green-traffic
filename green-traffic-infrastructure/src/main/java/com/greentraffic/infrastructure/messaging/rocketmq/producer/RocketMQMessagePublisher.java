package com.greentraffic.infrastructure.messaging.rocketmq.producer;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.infrastructure.messaging.reliability.MessageReliabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
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
public class RocketMQMessagePublisher implements MessagePublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final TaskExecutor taskExecutor;
    private final MessageReliabilityService reliabilityService;

    @Value("${messaging.rocketmq.topic:traffic-carbon}")
    private String defaultTopic;

    public RocketMQMessagePublisher(
            RocketMQTemplate rocketMQTemplate,
            @Qualifier("messagePublisherExecutor")
            TaskExecutor taskExecutor, MessageReliabilityService reliabilityService
    ) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.taskExecutor = taskExecutor;
        this.reliabilityService = reliabilityService;
    }

    @Override
    public void publish(Message<?> message) {
        if (reliabilityService.isDuplicate(message)) {
            log.info("消息 {} 已处理，跳过重复发送", message.getMessageId());
            return;
        }

        String destination = buildDestination(message);

        int attempts = 0;
        int max = reliabilityService.getMaxRetries();
        while (true) {
            try {
                SendResult result = rocketMQTemplate.syncSend(
                        destination,
                        buildSpringMessage(message)
                );
                log.info("RocketMQ 发送消息成功: {}, msgId: {}",
                        message.getMessageType(), result.getMsgId());
                reliabilityService.markSent(message);
                return;
            } catch (Exception e) {
                attempts++;
                log.error("RocketMQ 发送消息失败，attempt {}/{}", attempts, max, e);
                if (attempts > max) {
                    reliabilityService.handleDlq(message, e);
                    throw new RuntimeException("消息发送失败", e);
                }
                // retry
            }
        }
    }

    @Override
    public void publish(String topic, Message<?> message) {
        message.setTopic(topic);
        publish(message);
    }

    @Override
    public void publishAsync(Message<?> message) {
        if (reliabilityService.isDuplicate(message)) {
            log.info("消息 {} 已处理，跳过重复异步发送", message.getMessageId());
            return;
        }

        String destination = buildDestination(message);

        rocketMQTemplate.asyncSend(destination, buildSpringMessage(message),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("RocketMQ 异步发送成功: {}", sendResult.getMsgId());
                        reliabilityService.markSent(message);
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("RocketMQ 异步发送失败", e);
                        reliabilityService.handleDlq(message, e);
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
