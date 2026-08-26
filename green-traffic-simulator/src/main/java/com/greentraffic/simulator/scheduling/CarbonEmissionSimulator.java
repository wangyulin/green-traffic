package com.greentraffic.simulator.scheduling;

import com.greentraffic.core.port.input.SimulationOrchestrationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.invoke.MethodHandles;

@Component
@ConditionalOnProperty(prefix = "green-traffic.simulator", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CarbonEmissionSimulator {

    private final SimulationOrchestrationUseCase orchestrationUseCase;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CarbonEmissionSimulator(SimulationOrchestrationUseCase orchestrationUseCase) {
        this.orchestrationUseCase = orchestrationUseCase;
        logger.info("碳排放仿真系统-定时任务-初始化---");
    }

    @Scheduled(fixedDelayString = "${green-traffic.simulator.interval-ms:5000}")
    public void generateAndPublish() {
        logger.info("碳排放仿真系统-定时任务-触发---");
        orchestrationUseCase.generateAndPublish();
    }
}