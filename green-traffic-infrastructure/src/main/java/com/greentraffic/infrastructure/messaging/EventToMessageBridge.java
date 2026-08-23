package com.greentraffic.infrastructure.messaging;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventToMessageBridge {

    private final MessagePublisher publisher;

    public EventToMessageBridge(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onCarbonEmission(TrafficMetric event) {
        publisher.publish(Message.of(TrafficMessageTypes.CO2_EMISSION, event));
    }
}
