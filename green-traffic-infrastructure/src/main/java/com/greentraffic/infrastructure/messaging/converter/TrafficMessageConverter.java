package com.greentraffic.infrastructure.messaging.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;

import java.util.Set;
import java.util.Map;

@Component
public class TrafficMessageConverter
        implements MessagePayloadConverter<WriteTrafficMetricCommand> {

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

        // 如果 payload 已经是 WriteTrafficMetricCommand 或核心领域对象，允许转换
        if (payload instanceof WriteTrafficMetricCommand) {
            return true;
        }

        if (payload instanceof com.greentraffic.core.domain.traffic.TrafficMetric) {
            return true;
        }

        // 如果 payload 是仿真消息，应由 SimulationTrafficMessageConverter 处理。
        if (payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric) {
            return false;
        }

        if (payload instanceof Map<?, ?> map && map.containsKey("simulationId")) {
            return false;
        }

        // 其他 Map/JSON 物料也可尝试映射为 WriteTrafficMetricCommand
        return true;
    }

    @Override
    public Message<WriteTrafficMetricCommand> convert(Message<?> message) {
        if (message.getPayload() instanceof WriteTrafficMetricCommand cmd) {
            return copyWithPayload(message, cmd);
        }

        if (message.getPayload() instanceof com.greentraffic.core.domain.traffic.TrafficMetric domain) {
            WriteTrafficMetricCommand cmd = WriteTrafficMetricCommand.from(domain);
            return copyWithPayload(message, cmd);
        }

        // 尝试把 Map/JSON 映射为 WriteTrafficMetricCommand
        WriteTrafficMetricCommand cmd = objectMapper.convertValue(message.getPayload(), WriteTrafficMetricCommand.class);

        return copyWithPayload(message, cmd);
    }

    @Override
    public Set<String> supportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES;
    }

    private Message<WriteTrafficMetricCommand> copyWithPayload(
            Message<?> source,
            WriteTrafficMetricCommand payload) {

        return Message.<WriteTrafficMetricCommand>builder()
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