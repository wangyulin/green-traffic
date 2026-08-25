package com.greentraffic.simulator.sumo;

import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.output.simulation.SimulationEnginePort;
import com.greentraffic.core.port.output.simulation.SumoSimulationRequest;
import com.greentraffic.core.port.output.simulation.SumoTripInfo;
import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 驱动 SUMO 仿真的输入适配器；仿真结果只经消息端口离开模块。
 */
@Component
@ConditionalOnProperty(prefix = "green-traffic.sumo", name = "enabled", havingValue = "true")
public class SumoTrafficSimulator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SimulationEnginePort sumoSimulationPort;
    private final MessagePublisher messagePublisher;
    private final SumoSimulatorProperties properties;

    public SumoTrafficSimulator(
            SimulationEnginePort sumoSimulationPort,
            MessagePublisher messagePublisher,
            SumoSimulatorProperties properties) {
        this.sumoSimulationPort = sumoSimulationPort;
        this.messagePublisher = messagePublisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${green-traffic.sumo.interval-ms:10000}")
    public void simulateAndPublish() {
        logger.info("SUMO仿真系统-定时任务-触发---");
        String simulationId = UUID.randomUUID().toString();
        List<SumoTripInfo> trips = sumoSimulationPort.run(new SumoSimulationRequest(
                simulationId,
                properties.getWorkingDirectory(),
                properties.getDurationSeconds(),
                properties.getVehiclesPerHour()));
        SimulationTrafficMetric metric = SumoTrafficMetricMapper.map(simulationId, trips, Instant.now());
        if (metric != null) {
            try {
                messagePublisher.publishAsync(Message.of(TrafficMessageTypes.TRAFFIC_DATA_BATCH, metric));
            } catch (org.springframework.beans.factory.NoUniqueBeanDefinitionException ex) {
                logger.warn("检测到多个 TaskExecutor bean，回退使用同步 publish(): {}", ex.getMessage());
                messagePublisher.publish(Message.of(TrafficMessageTypes.TRAFFIC_DATA_BATCH, metric));
            }
        }
    }
}