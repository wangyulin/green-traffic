package com.greentraffic.infrastructure.messaging;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventToMessageBridge {

    private final MessagePublisher publisher;

    @Value("${messaging.type:events}")
    private String messagingType;

    public EventToMessageBridge(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onCarbonEmission(TrafficMetric event) {
        // 如果配置为 events，则不再外发（本地消费即可）
        if ("events".equalsIgnoreCase(messagingType)) {
            return;
        }

        // 转换成通用 Message（示例，具体结构按项目的 Message 定义）
        Message<TrafficMetric> msg = new Message<>();
        msg.setMessageType("carbon.emission");
        msg.setPayload(event);
        msg.setTopic("traffic.carbon");

        publisher.publish(msg); // MessageRouter 或具体 RocketMQPublisher 会处理
    }
}
