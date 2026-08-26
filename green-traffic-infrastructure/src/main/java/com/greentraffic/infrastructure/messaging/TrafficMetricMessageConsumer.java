package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
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

        if (payload instanceof TrafficMetric metric) {
            writeUseCase.write(
                    WriteTrafficMetricCommand.from(metric)
            );
            return;
        }

        // 支持来自 model 层或 core.domain 的仿真实体（优先使用 core.domain 类型）
        if (payload instanceof com.greentraffic.core.domain.traffic.SimulationTrafficMetric domainSim) {
            writeSimulationUseCase.write(
                WriteSimulationTrafficMetricCommand.from(domainSim)
            );
            return;
        }

        // 兼容来自外部 Adapter 的 Map/JSON 结构（例如 RocketMQ/HTTP 反序列化为 Map）
        if (payload instanceof java.util.Map<?, ?> map && map.containsKey("simulationId")) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            } catch (NoClassDefFoundError ignored) {
            }

            com.greentraffic.core.domain.traffic.SimulationTrafficMetric domainSim =
                mapper.convertValue(map, com.greentraffic.core.domain.traffic.SimulationTrafficMetric.class);

            writeSimulationUseCase.write(
                WriteSimulationTrafficMetricCommand.from(domainSim)
            );

            return;
        }

        log.warn(
                "Ignoring {} message with unsupported payload type: {}",
                message.getMessageType(),
                payload == null ? "null" : payload.getClass().getName()
        );
    }
}