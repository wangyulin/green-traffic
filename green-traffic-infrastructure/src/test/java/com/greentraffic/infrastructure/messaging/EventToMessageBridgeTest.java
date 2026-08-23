package com.greentraffic.infrastructure.messaging;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventToMessageBridgeTest {

    @Test
    void publishesCarbonEmissionAsUnifiedMessage() {
        MessagePublisher publisher = mock(MessagePublisher.class);
        EventToMessageBridge bridge = new EventToMessageBridge(publisher);
        TrafficMetric metric = new TrafficMetric(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, null,
                Instant.parse("2026-08-22T10:00:00Z")
        );

        bridge.onCarbonEmission(metric);

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(publisher).publish(messageCaptor.capture());
        Message<?> message = messageCaptor.getValue();
        assertThat(message.getMessageType()).isEqualTo(TrafficMessageTypes.CO2_EMISSION);
        assertThat(message.getPayload()).isSameAs(metric);
    }
}