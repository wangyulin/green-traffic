package com.greentraffic.infrastructure.messaging;

import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message 消息处理器（纯 handler，不负责订阅/注册）
 */
public class TrafficMetricMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TrafficMetricMessageConsumer.class);

    private final WriteTrafficMetricUseCase writeUseCase;
    private final WriteSimulationTrafficMetricUseCase writeSimulationUseCase;

    public TrafficMetricMessageConsumer(
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase) {

        this.writeUseCase = writeUseCase;
        this.writeSimulationUseCase = writeSimulationUseCase;
    }

    public void consume(Message<?> message) {
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

        // 兼容历史 Domain 对象：如果 Adapter 仍然发布 Domain 类型，转换为 Command 并处理
        if (payload instanceof SimulationTrafficMetric simMetric) {
            WriteSimulationTrafficMetricCommand simCmd = WriteSimulationTrafficMetricCommand.from(simMetric);
            writeSimulationUseCase.write(simCmd);
            return;
        }

        if (payload instanceof TrafficMetric metric) {
            WriteTrafficMetricCommand cmd = WriteTrafficMetricCommand.from(metric);
            writeUseCase.write(cmd);
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