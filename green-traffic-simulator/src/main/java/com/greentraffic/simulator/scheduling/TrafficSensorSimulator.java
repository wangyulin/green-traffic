package com.greentraffic.simulator.scheduling;

import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TrafficSensorSimulator {

    private final WriteTrafficMetricUseCase writeUseCase;

    // 模拟参数：基于当前代码中的示例值（120, 42.5），设置小幅波动范围
    private static final int VEHICLE_COUNT_MIN = 100;
    private static final int VEHICLE_COUNT_MAX = 140;
    private static final double SPEED_MIN = 37.5;
    private static final double SPEED_MAX = 47.5;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TrafficSensorSimulator(WriteTrafficMetricUseCase writeUseCase) {
        this.writeUseCase = writeUseCase;
    }

    // @Scheduled(fixedDelayString = "${green-traffic.simulator.interval-ms:5000}")
    // @Scheduled(fixedDelay = 5000)
    public void generateTrafficData() {
        logger.info("碳排放仿真系统-定时任务-触发---");
        // TODO: 调用 generator 生成模拟车流数据并发布/交给核心业务处理。
        int vehicleCount = ThreadLocalRandom.current().nextInt(VEHICLE_COUNT_MIN, VEHICLE_COUNT_MAX + 1);
        double speed = ThreadLocalRandom.current().nextDouble(SPEED_MIN, SPEED_MAX);
        // 保留一位小数
        double speedRounded = Math.round(speed * 10.0) / 10.0;

        TrafficMetric metric = new TrafficMetric(
            "ROAD-001",
            "EAST",
            null,
            vehicleCount,
            speedRounded,
            null,
            null,
            Instant.now()
        );

        writeUseCase.write(WriteTrafficMetricCommand.from(metric));
    }
}
