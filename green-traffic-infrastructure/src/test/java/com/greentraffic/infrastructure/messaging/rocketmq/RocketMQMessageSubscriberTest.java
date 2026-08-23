package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.rocketmq.consumer.RocketMQMessageSubscriber;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMQMessageSubscriberTest {

    @Test
    void convertsDeserializedMetricPayloadBeforeDispatching() {
        RocketMQMessageSubscriber subscriber = new RocketMQMessageSubscriber();
        AtomicReference<Message<?>> received = new AtomicReference<>();
        subscriber.subscribe(TrafficMessageTypes.CO2_EMISSION, received::set);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roadId", "ROAD-001");
        payload.put("direction", "EAST");
        payload.put("vehicleType", "CAR");
        payload.put("trafficFlow", 120);
        payload.put("averageSpeed", 42.5);
        payload.put("co2Emission", 12.3);
        payload.put("timestamp", "2026-08-22T10:00:00Z");

        subscriber.dispatchMessage(Message.of(TrafficMessageTypes.CO2_EMISSION, payload));

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getPayload()).isInstanceOf(TrafficMetric.class);
        TrafficMetric metric = (TrafficMetric) received.get().getPayload();
        assertThat(metric.roadId()).isEqualTo("ROAD-001");
        assertThat(metric.co2Emission()).isEqualTo(12.3);
    }

    @Test
    void dispatchesAlreadyTypedMetricWithoutConversion() {
        RocketMQMessageSubscriber subscriber = new RocketMQMessageSubscriber();
        AtomicReference<Message<?>> received = new AtomicReference<>();
        subscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA, received::set);
        TrafficMetric metric = new TrafficMetric("ROAD-002", "WEST", "BUS", 80, 31.0, 8.2, null, null);

        subscriber.dispatchMessage(Message.of(TrafficMessageTypes.TRAFFIC_DATA, metric));

        assertThat(received.get().getPayload()).isSameAs(metric);
    }
}