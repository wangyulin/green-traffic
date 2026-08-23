package com.greentraffic.simulator;

import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import com.greentraffic.common.util.TimezoneUtils;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CarbonEmissionSimulator {

    private final MessagePublisher messagePublisher;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // 模拟参数：基于当前代码中的示例值（120, 42.5），设置小幅波动范围
    private static final int VEHICLE_COUNT_MIN = 100;
    private static final int VEHICLE_COUNT_MAX = 140;
    private static final double SPEED_MIN = 37.5;
    private static final double SPEED_MAX = 47.5;

    public CarbonEmissionSimulator(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Scheduled(fixedDelayString = "${green-traffic.simulator.interval-ms:5000}")
    public void generateAndPublish() {
        logger.info("碳排放仿真系统-定时任务-触发---");
        // 调用 generator 生成模拟车流数据并发布/交给核心业务处理。
        int vehicleCount = ThreadLocalRandom.current().nextInt(VEHICLE_COUNT_MIN, VEHICLE_COUNT_MAX + 1);
        double speed = ThreadLocalRandom.current().nextDouble(SPEED_MIN, SPEED_MAX);

        double co2 = Math.round((vehicleCount * 0.12 + ThreadLocalRandom.current().nextDouble(-1.0, 1.0)) * 100.0) / 100.0;

        Instant ts = TimezoneUtils.normalizeInstant(Instant.now());

        TrafficMetric metric = new TrafficMetric(
            "ROAD-001",
            "EAST",
            null,
            vehicleCount,
            speed,
            co2,
            null,
            ts
        );

        // Send via MessagePublisher (implementation chosen by Spring via messaging.type)
        Message<com.greentraffic.model.entity.traffic.TrafficMetric> msg = Message.of(TrafficMessageTypes.CO2_EMISSION, metric);
        messagePublisher.publish(msg);
        logger.debug("Published simulated metric via MessagePublisher: {}", msg.getMessageId());
    }
}
