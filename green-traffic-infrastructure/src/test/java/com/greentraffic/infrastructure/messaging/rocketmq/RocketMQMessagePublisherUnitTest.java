package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.infrastructure.messaging.reliability.MessageReliabilityService;
import com.greentraffic.infrastructure.messaging.rocketmq.producer.RocketMQMessagePublisher;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * RocketMQMessagePublisher 单元测试。
 *
 * <p>
 * 测试 Infrastructure Adapter 的职责：
 * </p>
 *
 * <ul>
 *     <li>检查消息是否重复</li>
 *     <li>普通消息是否交给 RocketMQTemplate</li>
 *     <li>异步消息是否交给 RocketMQTemplate</li>
 *     <li>重复消息是否不会发送</li>
 * </ul>
 *
 * <p>
 * 本测试不连接真实 RocketMQ。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RocketMQMessagePublisherUnitTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private MessageReliabilityService reliabilityService;

    /**
     * 测试普通消息发送。
     *
     * 流程：
     *
     * Message
     *    ↓
     * ReliabilityService
     *    ↓
     * 非重复
     *    ↓
     * RocketMQTemplate.syncSend()
     */
    @Test
    void publishShouldCallRocketMQTemplate() {

        RocketMQMessagePublisher publisher =
                new RocketMQMessagePublisher(
                        rocketMQTemplate,
                        reliabilityService
                );

        Message<String> message = new Message<>();
        message.setMessageType("testType");
        message.setPayload("hello");

        /*
         * 当前消息不是重复消息。
         */
        when(reliabilityService.isDuplicate(message))
                .thenReturn(false);

        /*
         * RocketMQTemplate.syncSend() 返回 SendResult。
         *
         * 生产代码会调用：
         *
         * result.getMsgId()
         *
         * 所以这里不能让 Mockito 默认返回 null。
         */
        SendResult sendResult = mock(SendResult.class);

        when(sendResult.getMsgId())
                .thenReturn("test-msg-id");

        when(rocketMQTemplate.syncSend(
                anyString(),
                any(org.springframework.messaging.Message.class)
        )).thenReturn(sendResult);

        publisher.publish(message);

        /*
         * 验证可靠性服务被调用。
         */
        verify(reliabilityService)
                .isDuplicate(message);

        /*
         * 验证 RocketMQTemplate 被调用。
         */
        verify(rocketMQTemplate)
                .syncSend(
                        anyString(),
                        any(org.springframework.messaging.Message.class)
                );

    }

    /**
     * 测试异步消息发送。
     *
     * 当前架构：
     *
     * Message
     *    ↓
     * ReliabilityService
     *    ↓
     * 非重复
     *    ↓
     * RocketMQTemplate.asyncSend()
     *
     * 注意：
     *
     * RocketMQTemplate 本身已经提供 asyncSend，
     * 因此这里不要求 TaskExecutor.execute()。
     */
    @Test
    void publishAsyncShouldCallRocketMQTemplateAsync() {

        RocketMQMessagePublisher publisher =
                new RocketMQMessagePublisher(
                        rocketMQTemplate,
                        reliabilityService
                );

        Message<String> message = new Message<>();
        message.setMessageType("asyncType");
        message.setPayload("world");

        /*
         * 当前消息不是重复消息。
         */
        when(reliabilityService.isDuplicate(message))
                .thenReturn(false);

        publisher.publishAsync(message);

        /*
         * 验证可靠性检查。
         */
        verify(reliabilityService)
                .isDuplicate(message);

        /*
         * 验证真正调用 RocketMQ 异步发送。
         */
        verify(rocketMQTemplate)
                .asyncSend(
                        anyString(),
                        any(org.springframework.messaging.Message.class),
                        any()
                );

    }

    /**
     * 测试重复消息不会再次发送。
     *
     * 流程：
     *
     * Message
     *    ↓
     * isDuplicate() == true
     *    ↓
     * return
     *    ↓
     * RocketMQ 不发送
     */
    @Test
    void shouldNotPublishDuplicateMessage() {

        RocketMQMessagePublisher publisher =
                new RocketMQMessagePublisher(
                        rocketMQTemplate,
                        reliabilityService
                );

        Message<String> message = new Message<>();
        message.setMessageType("testType");
        message.setPayload("duplicate");

        when(reliabilityService.isDuplicate(message))
                .thenReturn(true);

        publisher.publish(message);

        verify(reliabilityService)
                .isDuplicate(message);

        verifyNoInteractions(rocketMQTemplate);
    }
}
