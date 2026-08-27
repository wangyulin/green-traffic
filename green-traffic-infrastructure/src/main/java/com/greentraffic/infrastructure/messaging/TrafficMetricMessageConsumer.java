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
        // 现在严格只接受 Command 层面的载荷；入站 Adapter/Subscriber 应该通过
        // MessagePayloadConverter 将外部 DTO/Map/JSON 转换为 Command。
        if (payload instanceof WriteTrafficMetricCommand cmd) {
            writeUseCase.write(cmd);
            return;
        }

        if (payload instanceof WriteSimulationTrafficMetricCommand simCmd) {
            writeSimulationUseCase.write(simCmd);
            return;
        }

        log.warn(
                "Ignoring {} message with unsupported payload type: {}",
                message.getMessageType(),
                payload == null ? "null" : payload.getClass().getName()
        );
    }
}