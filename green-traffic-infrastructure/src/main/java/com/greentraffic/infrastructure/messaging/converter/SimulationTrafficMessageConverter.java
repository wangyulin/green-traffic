package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;

import java.util.Set;

public class SimulationTrafficMessageConverter
        implements MessagePayloadConverter<SimulationTrafficMetric> {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of(
            TrafficMessageTypes.TRAFFIC_DATA_BATCH
    );

    private final ObjectMapper objectMapper;

    public SimulationTrafficMessageConverter(ObjectMapper objectMapper) {
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

        return !(message.getPayload() instanceof SimulationTrafficMetric);
    }

    @Override
    public Message<SimulationTrafficMetric> convert(Message<?> message) {
        if (message.getPayload() instanceof SimulationTrafficMetric metric) {
            return copyWithPayload(message, metric);
        }

        SimulationTrafficMetric metric =
                objectMapper.convertValue(
                        message.getPayload(),
                        SimulationTrafficMetric.class
                );

        return copyWithPayload(message, metric);
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    private Message<SimulationTrafficMetric> copyWithPayload(
            Message<?> source,
            SimulationTrafficMetric payload) {

        return Message.<SimulationTrafficMetric>builder()
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