package com.greentraffic.core.application;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.domain.simulation.SimulationTrafficGenerator;
import com.greentraffic.core.port.input.SimulationOrchestrationUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// core is framework-agnostic; keep only java standard imports when needed

/**
 * 将仿真计算逻辑迁移到 core 的实现，负责生成仿真数据并发布消息。
 */
public class SimulationOrchestrationService implements SimulationOrchestrationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MessagePublisher messagePublisher;
    private final SimulationTrafficGenerator generator;
    private final com.greentraffic.core.port.util.IdGenerator idGenerator;
    private final java.time.Clock clock;

    public SimulationOrchestrationService(MessagePublisher messagePublisher, SimulationTrafficGenerator generator,
                                           com.greentraffic.core.port.util.IdGenerator idGenerator, java.time.Clock clock) {
        this.messagePublisher = messagePublisher;
        this.generator = generator;
        this.idGenerator = idGenerator;
        this.clock = clock;
        logger.info("SimulationOrchestrationService initialized");
    }

    @Override
    public void generateAndPublish() {
        logger.info("Simulation orchestration triggered");
        SimulationTrafficMetric metric = generator.generate();
        Message<SimulationTrafficMetric> msg = Message.of(TrafficMessageTypes.CO2_EMISSION, metric, idGenerator, clock);
        try {
            messagePublisher.publishAsync(msg);
        } catch (Exception ex) {
            logger.warn("publishAsync failed, falling back to publish(): {}", ex.getMessage());
            messagePublisher.publish(msg);
        }
        logger.debug("Published simulated metric via core use-case: {}", msg.getMessageId());
    }
}
