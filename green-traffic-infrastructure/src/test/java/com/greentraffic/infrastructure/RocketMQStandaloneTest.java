package com.greentraffic.infrastructure;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone RocketMQ send/receive test.
 * - 不依赖项目业务代码；直接使用 RocketMQ 客户端 API。
 * - 通过系统属性 `rocketmq.namesrv` 和 `rocketmq.topic` 可覆盖地址与主题。
 *
 * 运行示例:
 * mvn -pl green-traffic-infrastructure -Dtest=RocketMQStandaloneTest test -Drocketmq.namesrv=127.0.0.1:9876
 */
public class RocketMQStandaloneTest {

    private static final String NAMESRV = System.getProperty("rocketmq.namesrv", "127.0.0.1:9876");
    private static final String TOPIC = System.getProperty("rocketmq.topic", "green_traffic_v1");

    private static DefaultMQProducer producer;
    private static DefaultMQPushConsumer consumer;

    @BeforeAll
    public static void setup() throws Exception {
        producer = new DefaultMQProducer("test-producer-" + UUID.randomUUID());
        producer.setNamesrvAddr(NAMESRV);
        producer.start();

        consumer = new DefaultMQPushConsumer("test-consumer-" + UUID.randomUUID());
        consumer.setNamesrvAddr(NAMESRV);
    }

    @AfterAll
    public static void tearDown() {
        if (producer != null) {
            try { producer.shutdown(); } catch (Exception ignored) {}
        }
        if (consumer != null) {
            try { consumer.shutdown(); } catch (Exception ignored) {}
        }
    }

    @Test
    public void testSendAndReceive() throws Exception {
        String payload = "{\"source\":\"standalone-test\",\"value\":123,\"ts\":" + System.currentTimeMillis() + "}";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // 使用唯一 tag 避免消费到其他来源的消息
        String tag = "testTag-" + UUID.randomUUID();
        consumer.subscribe(TOPIC, tag);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            try {
                if (msgs != null && !msgs.isEmpty()) {
                    Message msg = msgs.get(0);
                    String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                    received.set(body);
                    latch.countDown();
                }
            } catch (Exception e) {
                // ignore here — test will fail on timeout or assertion
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // 启动 consumer，在注册监听器后启动以确保 listener 可用
        consumer.start();

        // 使用同一 tag 发送
        Message msg = new Message(TOPIC, tag, payload.getBytes(StandardCharsets.UTF_8));

        SendResult result = producer.send(msg);
        Assertions.assertNotNull(result);

        boolean ok = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(ok, "没有在 10 秒内收到消息，请确认 RocketMQ 名称服务地址和 broker 是否可连通: " + NAMESRV);

        Assertions.assertEquals(payload, received.get(), "接收到的消息体与发送的不一致");
    }
}
