package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.model.entity.traffic.TrafficMetric;

import java.util.Set;

public class TrafficMessageConverter
        implements MessagePayloadConverter<TrafficMetric> {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of(
            TrafficMessageTypes.TRAFFIC_DATA,
            TrafficMessageTypes.CO2_EMISSION
    );

    private final ObjectMapper objectMapper;

    public TrafficMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(Message<?> message) {
        if (message == null) {
            return false;
        }

        if (!SUPPORTED_MESSAGE_TYPES.contains(message.getMessageType())) {
            return false;
        }

        return !(message.getPayload() instanceof TrafficMetric);
    }

    @Override
    public Message<TrafficMetric> convert(Message<?> message) {
        if (message.getPayload() instanceof TrafficMetric metric) {
            return copyWithPayload(message, metric);
        }

        TrafficMetric metric =
                objectMapper.convertValue(
                        message.getPayload(),
                        TrafficMetric.class
                );

        return copyWithPayload(message, metric);
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    private Message<TrafficMetric> copyWithPayload(
            Message<?> source,
            TrafficMetric payload) {

        return Message.<TrafficMetric>builder()
                .messageId(source.getMessageId())
                .messageType(source.getMessageType())
                .topic(source.getTopic())
                .tag(source.getTag())
                .key(source.getKey())
                .payload(payload)
                .headers(source.getHeaders())
                .timestamp(source.getTimestamp())
                .schemaVersion(source.getSchemaVersion())
                .source(source.getSource())
                .traceId(source.getTraceId())
                .correlationId(source.getCorrelationId())
                .build();
    }
}