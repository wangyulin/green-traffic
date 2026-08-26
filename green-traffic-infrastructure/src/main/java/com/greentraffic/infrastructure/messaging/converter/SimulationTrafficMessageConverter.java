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
                || payload instanceof com.greentraffic.model.entity.traffic.SimulationTrafficMetric
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

        if (payload instanceof com.greentraffic.model.entity.traffic.SimulationTrafficMetric metric) {
            return copyWithPayload(
                    message,
                    toDomainMetric(metric)
            );
        }

        if (payload == null) {
            throw new IllegalArgumentException(
                    "Cannot convert null payload to SimulationTrafficMetric"
            );
        }

        /*
         * 对于 RocketMQ / JSON 反序列化后的 Map 等类型，
         * 仍然允许 ObjectMapper 做一次转换。
         *
         * 但是 model.entity.traffic.SimulationTrafficMetric
         * 已经在上面显式转换，因此不会再触发 Instant 的 Jackson
         * 序列化/反序列化问题。
         */
        com.greentraffic.model.entity.traffic.SimulationTrafficMetric modelMetric =
                objectMapper.convertValue(
                        payload,
                        com.greentraffic.model.entity.traffic.SimulationTrafficMetric.class
                );

        return copyWithPayload(
                message,
                toDomainMetric(modelMetric)
        );
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    private SimulationTrafficMetric toDomainMetric(
            com.greentraffic.model.entity.traffic.SimulationTrafficMetric source) {

        return new SimulationTrafficMetric(
                source.simulationId(),
                source.roadId(),
                source.direction(),
                source.vehicleType(),
                source.vehicleCount(),
                source.averageSpeed(),
                source.totalCo2Emission(),
                source.averageTravelTime(),
                source.averageWaitingTime(),
                source.averageTimeLoss(),
                source.totalRouteLength(),
                source.timestamp()
        );
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