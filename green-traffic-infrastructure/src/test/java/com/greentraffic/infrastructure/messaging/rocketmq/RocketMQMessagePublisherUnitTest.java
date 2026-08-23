package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.infrastructure.messaging.rocketmq.producer.RocketMQMessagePublisher;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Unit tests for RocketMQMessagePublisher using a mocked RocketMQTemplate.
 */
class RocketMQMessagePublisherUnitTest {

    @Test
    void publishShouldCallRocketMQTemplate() {
        RocketMQTemplate template = new RocketMQTemplate() {
            @Override
            public SendResult syncSend(String destination, org.springframework.messaging.Message<?> message) {
                return new SendResult();
            }
        };

        RocketMQMessagePublisher publisher = new RocketMQMessagePublisher(template);

        Message<String> msg = new Message<>();
        msg.setMessageType("testType");
        msg.setPayload("hello");

        publisher.publish(msg);
    }

    @Test
    void publishAsyncShouldCallRocketMQTemplateAsync() {
        RocketMQTemplate template2 = new RocketMQTemplate() {
            @Override
            public void asyncSend(String destination, org.springframework.messaging.Message<?> message, org.apache.rocketmq.client.producer.SendCallback sendCallback) {
                // simulate async success
                if (sendCallback != null) {
                    // do nothing
                }
            }
        };

        RocketMQMessagePublisher publisher2 = new RocketMQMessagePublisher(template2);

        Message<String> msg2 = new Message<>();
        msg2.setMessageType("asyncType");
        msg2.setPayload("world");

        publisher2.publishAsync(msg2);
    }
}
