package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.domain.traffic.TrafficMetric;

import java.util.Set;
import java.util.Map;

@Component
public class TrafficMessageConverter
        implements MessagePayloadConverter<TrafficMetric> {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of(
            TrafficMessageTypes.TRAFFIC_DATA,
            TrafficMessageTypes.CO2_EMISSION
    );

    private final ObjectMapper objectMapper;

    public TrafficMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } catch (NoClassDefFoundError ignored) {
        }
        this.objectMapper = mapper;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public TrafficMessageConverter(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            } catch (NoClassDefFoundError ignored) {
            }
            this.objectMapper = mapper;
        } else {
            this.objectMapper = objectMapper;
        }
    }

    @Override
    public boolean supports(Message<?> message) {
        if (message == null) {
            return false;
        }

        if (!SUPPORTED_MESSAGE_TYPES.contains(message.getMessageType())) {
            return false;
        }

        Object payload = message.getPayload();

        // 如果 payload 已经是核心领域对象，说明无需转换
        if (payload instanceof TrafficMetric) {
            return false;
        }

        // 如果 payload 显式是仿真模型（来自 simulator 的 model 层），
        // 或者是反序列化得到的 Map 且包含 simulationId 字段，
        // 那说明这是一条仿真消息，应由 SimulationTrafficMessageConverter 处理。
        if (payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric) {
            return false;
        }

        if (payload instanceof Map<?, ?> map && map.containsKey("simulationId")) {
            return false;
        }

        return true;
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