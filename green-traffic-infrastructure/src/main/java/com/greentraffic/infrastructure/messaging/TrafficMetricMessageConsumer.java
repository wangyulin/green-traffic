package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TrafficMetricMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TrafficMetricMessageConsumer.class);

    private final MessageSubscriber messageSubscriber;

    private final WriteTrafficMetricUseCase writeUseCase;
    private final WriteSimulationTrafficMetricUseCase writeSimulationUseCase;

    public TrafficMetricMessageConsumer(
            MessageSubscriber messageSubscriber,
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase) {

        this.messageSubscriber = messageSubscriber;
        this.writeUseCase = writeUseCase;
        this.writeSimulationUseCase = writeSimulationUseCase;
    }

    @PostConstruct
    void subscribe() {
        messageSubscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA,
                this::consume
        );

        messageSubscriber.subscribe(
                TrafficMessageTypes.CO2_EMISSION,
                this::consume
        );
    }

    @PreDestroy
    void unsubscribe() {
        messageSubscriber.unsubscribe(
                TrafficMessageTypes.TRAFFIC_DATA
        );

        messageSubscriber.unsubscribe(
                TrafficMessageTypes.CO2_EMISSION
        );
    }

    private void consume(Message<?> message) {
        log.info("TrafficMetricMessageConsumer : 收到消息");
        if (message == null) {
            log.warn("Ignoring null traffic metric message");
            return;
        }
        Object payload = message.getPayload();

        // 优先处理传输层的 Command 对象（Adapter 应该发布 Command/DTO 而不是直接 Domain）
        if (payload instanceof WriteTrafficMetricCommand cmd) {
            writeUseCase.write(cmd);
            return;
        }

        if (payload instanceof WriteSimulationTrafficMetricCommand simCmd) {
            writeSimulationUseCase.write(simCmd);
            return;
        }

        // 兼容历史 Map 载荷：将 Map 转换为 Command（移除对 domain 类型的直接引用）
        if (payload instanceof Map<?, ?> map) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            } catch (NoClassDefFoundError ignored) {
            }

            if (map.containsKey("simulationId")) {
                WriteSimulationTrafficMetricCommand simCmd = mapper.convertValue(map, WriteSimulationTrafficMetricCommand.class);
                writeSimulationUseCase.write(simCmd);
                return;
            } else {
                WriteTrafficMetricCommand cmd = mapper.convertValue(map, WriteTrafficMetricCommand.class);
                writeUseCase.write(cmd);
                return;
            }
        }

        log.warn(
                "Ignoring {} message with unsupported payload type: {}",
                message.getMessageType(),
                payload == null ? "null" : payload.getClass().getName()
        );
    }
}