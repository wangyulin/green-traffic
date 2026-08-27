package com.greentraffic.infrastructure.messaging.rocketmq;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.converter.MessagePayloadConverter;
import com.greentraffic.infrastructure.messaging.rocketmq.consumer.RocketMQMessageSubscriber;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RocketMQMessageSubscriber 单元测试。
 *
 * <p>本测试只验证 Subscriber 的职责：</p>
 * <ul>
 *     <li>消息订阅</li>
 *     <li>消息路由</li>
 *     <li>调用 MessagePayloadConverter</li>
 *     <li>将转换后的消息交给 Handler</li>
 * </ul>
 *
 * <p>具体的 Payload 转换逻辑由：</p>
 * <ul>
 *     <li>TrafficMessageConverterTest</li>
 *     <li>SimulationTrafficMessageConverterTest</li>
 * </ul>
 * 单独测试。
 */
@ExtendWith(MockitoExtension.class)
class RocketMQMessageSubscriberTest {

        @Mock
        private MessagePayloadConverter<?> converter;

    private RocketMQMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new RocketMQMessageSubscriber(
                List.of(converter)
        );
    }

    /**
     * 测试已经是领域对象的消息：
     *
     * Message<TrafficMetric>
     *        ↓
     * Subscriber
     *        ↓
     * Handler
     *
     * 不需要验证具体的 TrafficMetric 转换逻辑。
     */
    @Test
    void dispatchesAlreadyTypedMetricToRegisteredHandler() {

        TrafficMetric metric = new TrafficMetric(
                "ROAD-002",
                "WEST",
                "BUS",
                80,
                31.0,
                8.2,
                null,
                null
        );

        Message<TrafficMetric> message =
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        metric
                );

        AtomicReference<Message<?>> received =
                new AtomicReference<>();

        subscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA,
                received::set
        );

        when(converter.supports(message))
                .thenReturn(false);

        subscriber.dispatchMessage(message);

        assertThat(received.get())
                .isNotNull();

        assertThat(received.get())
                .isSameAs(message);

        assertThat(received.get().getPayload())
                .isSameAs(metric);

        verify(converter)
                .supports(message);

        verify(converter, never())
                .convert(any());
    }

    /**
     * 测试 Subscriber 能够使用 Converter。
     *
     * 流程：
     *
     * Message<Map>
     *       ↓
     * Subscriber
     *       ↓
     * Converter.supports()
     *       ↓
     * Converter.convert()
     *       ↓
     * Message<TrafficMetric>
     *       ↓
     * Handler
     */
    @Test
    void convertsMessageBeforeDispatchingToHandler() {

        Message<?> sourceMessage =
                Message.of(
                        TrafficMessageTypes.CO2_EMISSION,
                        java.util.Map.of(
                                "roadId", "ROAD-001",
                                "direction", "EAST",
                                "vehicleType", "CAR",
                                "trafficFlow", 120,
                                "averageSpeed", 42.5,
                                "co2Emission", 12.3
                        )
                );

        TrafficMetric metric = new TrafficMetric(
                "ROAD-001",
                "EAST",
                "CAR",
                120,
                42.5,
                12.3,
                null,
                null
        );

        Message<TrafficMetric> convertedMessage =
                sourceMessage.withPayload(metric);

        AtomicReference<Message<?>> received =
                new AtomicReference<>();

        subscriber.subscribe(
                TrafficMessageTypes.CO2_EMISSION,
                received::set
        );

        when(converter.supports(sourceMessage))
                .thenReturn(true);

        org.mockito.Mockito.<Message<?>>when(converter.convert(sourceMessage))
                .thenReturn(convertedMessage);

        subscriber.dispatchMessage(sourceMessage);

        assertThat(received.get())
                .isNotNull();

        assertThat(received.get())
                .isSameAs(convertedMessage);

        assertThat(received.get().getPayload())
                .isSameAs(metric);

        verify(converter)
                .supports(sourceMessage);

        verify(converter)
                .convert(sourceMessage);
    }

    /**
     * 测试当没有 Converter 支持当前消息时，
     * Subscriber 应该直接将原始消息交给 Handler。
     */
    @Test
    void dispatchesOriginalMessageWhenNoConverterSupportsIt() {

        Message<TrafficMetric> message =
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        new TrafficMetric(
                                "ROAD-003",
                                "NORTH",
                                "TRUCK",
                                50,
                                28.0,
                                15.5,
                                null,
                                null
                        )
                );

        AtomicReference<Message<?>> received =
                new AtomicReference<>();

        subscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA,
                received::set
        );

        when(converter.supports(message))
                .thenReturn(false);

        subscriber.dispatchMessage(message);

        assertThat(received.get())
                .isNotNull();

        assertThat(received.get())
                .isSameAs(message);

        verify(converter)
                .supports(message);

        verify(converter, never())
                .convert(any());
    }

    /**
     * 测试没有注册 Handler 时，Subscriber 不应该抛异常。
     */
    @Test
    void doesNothingWhenNoHandlerIsRegistered() {

        Message<TrafficMetric> message =
                Message.of(
                        TrafficMessageTypes.TRAFFIC_DATA,
                        new TrafficMetric(
                                "ROAD-004",
                                "SOUTH",
                                "CAR",
                                100,
                                35.0,
                                10.0,
                                null,
                                null
                        )
                );

        when(converter.supports(message))
                .thenReturn(false);

        subscriber.dispatchMessage(message);

        verify(converter)
                .supports(message);

        verify(converter, never())
                .convert(any());
    }
}