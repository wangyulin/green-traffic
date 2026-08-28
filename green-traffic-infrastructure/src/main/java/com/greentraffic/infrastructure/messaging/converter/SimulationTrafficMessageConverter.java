package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;

import java.util.Map;
import java.util.Set;

@Component
public class SimulationTrafficMessageConverter
        implements MessagePayloadConverter<WriteSimulationTrafficMetricCommand> {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of(
            TrafficMessageTypes.TRAFFIC_DATA_BATCH,
            TrafficMessageTypes.TRAFFIC_DATA,
            TrafficMessageTypes.CO2_EMISSION
    );

    private final ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public SimulationTrafficMessageConverter(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            } catch (NoClassDefFoundError ignored) {
                // jackson-datatype-jsr310 may not be on the classpath in some test setups
            }
            this.objectMapper = mapper;
        } else {
            this.objectMapper = objectMapper;
        }
    }

    // 保留无参构造器以兼容现有单元测试或手工实例化场景
    public SimulationTrafficMessageConverter() {
        this(null);
    }

    @Override
    public boolean supports(Message<?> message) {
        if (message == null || !SUPPORTED_MESSAGE_TYPES.contains(message.getMessageType())) {
            return false;
        }

        Object payload = message.getPayload();
        return payload instanceof WriteSimulationTrafficMetricCommand
            || payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric
            || payload instanceof Map<?, ?> map && map.containsKey("simulationId");
    }

    @Override
    public Message<WriteSimulationTrafficMetricCommand> convert(Message<?> message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        Object payload = message.getPayload();

        if (payload instanceof WriteSimulationTrafficMetricCommand cmd) {
            return copyWithPayload(message, cmd);
        }

        if (payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric metric) {
            return copyWithPayload(message, WriteSimulationTrafficMetricCommand.from(metric));
        }

        if (payload == null) {
            throw new IllegalArgumentException(
                "Cannot convert null payload to WriteSimulationTrafficMetricCommand"
            );
        }

        // 对于 Map 或 JSON 字符串等情况，尝试直接映射为 WriteSimulationTrafficMetricCommand
        WriteSimulationTrafficMetricCommand cmd =
            objectMapper.convertValue(
                payload,
                WriteSimulationTrafficMetricCommand.class
            );

        return copyWithPayload(
            message,
            cmd
        );
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    private Message<WriteSimulationTrafficMetricCommand> copyWithPayload(
            Message<?> source,
            WriteSimulationTrafficMetricCommand payload) {

        return Message.<WriteSimulationTrafficMetricCommand>builder()
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