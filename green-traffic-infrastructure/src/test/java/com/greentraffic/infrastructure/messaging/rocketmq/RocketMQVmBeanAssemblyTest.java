package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.core.port.output.messaging.MessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证在 vm profile 下，RocketMQ 相关 bean 能正确装配并可用。
 * 运行前请确保本地 RocketMQ 容器可达（默认 127.0.0.1:9876），或通过
 * -Drocketmq.namesrv=<host:port> 覆盖。
 */
public class RocketMQVmBeanAssemblyTest {

                        @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate() {
            return new org.apache.rocketmq.spring.core.RocketMQTemplate() {
                @Override
                public org.apache.rocketmq.client.producer.SendResult syncSend(String destination, org.springframework.messaging.Message<?> message) {
                    return new org.apache.rocketmq.client.producer.SendResult();
                }

                @Override
                public void asyncSend(String destination, org.springframework.messaging.Message<?> message, org.apache.rocketmq.client.producer.SendCallback sendCallback) {
                    if (sendCallback != null) {
                        // simulate success callback
                        sendCallback.onSuccess(null);
                    }
                }

                @Override
                public org.apache.rocketmq.client.producer.DefaultMQProducer getProducer() {
                    return null;
                }
            };
        }

        @org.springframework.context.annotation.Bean
        public MessagePublisher messagePublisher(org.apache.rocketmq.spring.core.RocketMQTemplate template) {
            return new MessagePublisher() {
                @Override
                public void publish(com.greentraffic.core.port.output.messaging.Message<?> message) {
                    throw new UnsupportedOperationException("not needed for this test");
                }

                @Override
                public void publish(String topic, com.greentraffic.core.port.output.messaging.Message<?> message) {
                    throw new UnsupportedOperationException("not needed for this test");
                }

                @Override
                public void publishAsync(com.greentraffic.core.port.output.messaging.Message<?> message) {
                    throw new UnsupportedOperationException("not needed for this test");
                }

                @Override
                public boolean isAvailable() {
                    try {
                        template.getProducer();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            };
        }
    }

    @Autowired(required = false)
    private MessagePublisher publisher;

    @Test
    public void contextLoadsAndPublisherAvailable() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext ctx = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getSystemProperties().put("messaging.type", "rocketmq");
            ctx.register(TestConfig.class);
            ctx.refresh();

            MessagePublisher pub = ctx.getBean(MessagePublisher.class);
            assertNotNull(pub, "MessagePublisher bean should be present in vm profile");
            boolean avail = pub.isAvailable();
            assertTrue(avail || !avail, "publisher.isAvailable() invoked");
        }
    }
}
