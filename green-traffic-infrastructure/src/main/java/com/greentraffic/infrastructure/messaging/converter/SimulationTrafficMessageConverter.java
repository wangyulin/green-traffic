package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;

import java.util.Map;
import java.util.Set;

@Component
public class SimulationTrafficMessageConverter
        implements MessagePayloadConverter<SimulationTrafficMetric> {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of(
            TrafficMessageTypes.TRAFFIC_DATA_BATCH,
            TrafficMessageTypes.TRAFFIC_DATA,
            TrafficMessageTypes.CO2_EMISSION
    );

    private final ObjectMapper objectMapper;

    public SimulationTrafficMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } catch (NoClassDefFoundError ignored) {
            // jackson-datatype-jsr310 may not be on the classpath in some test setups
        }
        this.objectMapper = mapper;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public SimulationTrafficMessageConverter(ObjectMapper objectMapper) {
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
        if (message == null || !SUPPORTED_MESSAGE_TYPES.contains(message.getMessageType())) {
            return false;
        }

        Object payload = message.getPayload();
        return payload instanceof SimulationTrafficMetric
            || payload instanceof Map<?, ?> map && map.containsKey("simulationId");
    }

    @Override
    public Message<SimulationTrafficMetric> convert(Message<?> message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        Object payload = message.getPayload();

        if (payload instanceof SimulationTrafficMetric metric) {
            return copyWithPayload(message, metric);
        }

        if (payload == null) {
            throw new IllegalArgumentException(
                "Cannot convert null payload to SimulationTrafficMetric"
            );
        }

        // 对于 Map 或 JSON 字符串等情况，直接映射为 core.domain.SimulationTrafficMetric
        SimulationTrafficMetric modelMetric =
            objectMapper.convertValue(
                payload,
                SimulationTrafficMetric.class
            );

        return copyWithPayload(
            message,
            modelMetric
        );
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    // no-op: mapping now converts directly to core.domain.SimulationTrafficMetric

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