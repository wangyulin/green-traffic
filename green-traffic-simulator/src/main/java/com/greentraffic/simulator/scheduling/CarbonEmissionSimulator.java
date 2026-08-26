package com.greentraffic.simulator.scheduling;

import com.greentraffic.model.entity.traffic.SimulationTrafficMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.*;

import com.greentraffic.common.util.TimezoneUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CarbonEmissionSimulator {

    private final MessagePublisher messagePublisher;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // ============ 模拟参数 ============
    private static final double FREE_FLOW_SPEED = 60.0;  // km/h，自由流速度
    private static final double JAM_DENSITY = 2000.0;    // 辆/km，堵塞密度
    private static final double ROAD_LENGTH = 15.0;      // km，道路长度

    // 车辆类型及其 CO2 排放系数（g/km）
    private static final Map<String, Double> CO2_EMISSION_FACTORS = Map.of(
            "passenger", 0.12,      // 小客车
            "truck", 0.35,          // 货车
            "bus", 0.25,            // 公交车
            "motorcycle", 0.06      // 摩托车
    );

    // 车辆类型权重（用于随机选择）
    private static final String[] VEHICLE_TYPES = {"passenger", "passenger", "passenger", "truck", "bus", "motorcycle"};

    private static final String[] DIRECTIONS = {"NORTH", "SOUTH", "EAST", "WEST", "UNKNOWN"};
    private static final String ROAD_ID = "Carbon-GRID";

    public CarbonEmissionSimulator(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
        logger.info("碳排放仿真系统-定时任务-初始化---");
    }

    @Scheduled(fixedDelayString = "${green-traffic.simulator.interval-ms:5000}")
    public void generateAndPublish() {
        logger.info("碳排放仿真系统-定时任务-触发---");

        SimulationTrafficMetric simulationTrafficMetric = generateTrafficMetric();

        Message<SimulationTrafficMetric> msg = Message.of(TrafficMessageTypes.CO2_EMISSION, simulationTrafficMetric);
        try {
            messagePublisher.publishAsync(msg);
        } catch (NoUniqueBeanDefinitionException ex) {
            logger.warn("检测到多个 TaskExecutor bean，回退使用同步 publish(): {}", ex.getMessage());
            messagePublisher.publish(msg);
        }
        logger.debug("Published simulated metric via MessagePublisher: {}", msg.getMessageId());
    }

    /**
     * 生成一条完整的模拟交通数据
     */
    private SimulationTrafficMetric generateTrafficMetric() {
        // 1. 生成基础数据
        String simulationId = UUID.randomUUID().toString();
        String vehicleType = VEHICLE_TYPES[ThreadLocalRandom.current().nextInt(VEHICLE_TYPES.length)];
        String direction = DIRECTIONS[ThreadLocalRandom.current().nextInt(DIRECTIONS.length)];

        // 2. 生成车流量：模拟早晚高峰
        int vehicleCount = generateVehicleCount();

        // 3. 生成平均速度：流量越大速度越低
        double averageSpeed = generateAverageSpeed(vehicleCount);

        // 4. 计算平均行程时间
        double averageTravelTime = calculateTravelTime(averageSpeed);

        // 5. 计算等待时间：速度越低等待时间越长
        double averageWaitingTime = calculateWaitingTime(averageSpeed);

        // 6. 计算时间损失
        double averageTimeLoss = calculateTimeLoss(averageTravelTime);

        // 7. 计算总路线长度
        double totalRouteLength = vehicleCount * ROAD_LENGTH * 1000; // 转换为米

        // 8. 计算 CO2 排放总量（kg）
        double totalCo2Emission = calculateCo2Emission(vehicleCount, vehicleType, totalRouteLength);

        // 9. 时间戳
        Instant ts = TimezoneUtils.normalizeInstant(Instant.now());

        // 10. 构建 TrafficMetric
        return new SimulationTrafficMetric(
                simulationId,
                ROAD_ID,
                direction,
                vehicleType,
                vehicleCount,
                round(averageSpeed, 2),
                round(totalCo2Emission, 3),
                round(averageTravelTime, 2),
                round(averageWaitingTime, 2),
                round(averageTimeLoss, 2),
                round(totalRouteLength, 2),
                ts
        );
    }

    /**
     * 生成车流量：模拟早晚高峰
     * 早高峰：7:00-9:00，晚高峰：17:00-19:00
     */
    private int generateVehicleCount() {
        // 修复：使用 ZonedDateTime 获取完整的日期时间信息
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        double peakFactor = getPeakFactor(currentTime, dayOfWeek);

        // 基础流量：500 辆/小时
        int baseFlow = 500;

        // 随机波动 ±20%
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.8, 1.2);

        // 最终流量 = 基础流量 × 高峰系数 × 随机波动
        int vehicleCount = (int) Math.round(baseFlow * peakFactor * randomFactor);

        // 限制范围
        return Math.max(50, Math.min(2000, vehicleCount));
    }

    /**
     * 获取高峰系数
     */
    private double getPeakFactor(LocalTime time, DayOfWeek dayOfWeek) {
        int hour = time.getHour();
        int minute = time.getMinute();
        double timeInHours = hour + minute / 60.0;

        // 工作日和周末不同的流量模式
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

        if (isWeekend) {
            // 周末：中午和傍晚流量较大
            if (timeInHours >= 10 && timeInHours < 14) {
                return 1.2;  // 中午高峰
            } else if (timeInHours >= 16 && timeInHours < 19) {
                return 1.3;  // 傍晚高峰
            } else if (timeInHours >= 22 || timeInHours < 6) {
                return 0.3;  // 夜间低谷
            } else {
                return 0.8;  // 其他时间
            }
        } else {
            // 工作日：早晚高峰明显
            if (timeInHours >= 7 && timeInHours < 9) {
                return 1.8;  // 早高峰
            } else if (timeInHours >= 17 && timeInHours < 19) {
                return 2.0;  // 晚高峰
            } else if (timeInHours >= 9 && timeInHours < 17) {
                return 1.0;  // 白天平峰
            } else if (timeInHours >= 19 && timeInHours < 22) {
                return 1.4;  // 晚间
            } else {
                return 0.2;  // 深夜
            }
        }
    }

    /**
     * 生成平均速度：流量越大速度越低
     * 使用 BPR 函数（美国公路局函数）
     */
    private double generateAverageSpeed(int vehicleCount) {
        // 道路通行能力
        double capacity = 1500.0; // 辆/小时

        // 流量/通行能力比
        double vcRatio = vehicleCount / capacity;

        // BPR 函数计算行程时间
        double travelTimeFactor = 1 + 0.15 * Math.pow(vcRatio, 4);

        // 实际速度 = 自由流速度 / 时间系数
        double baseSpeed = FREE_FLOW_SPEED / travelTimeFactor;

        // 添加随机波动 ±15%
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
        double speed = baseSpeed * randomFactor;

        // 限制速度范围
        return Math.max(5.0, Math.min(FREE_FLOW_SPEED, speed));
    }

    /**
     * 计算平均行程时间（分钟）
     */
    private double calculateTravelTime(double averageSpeed) {
        // 行程时间 = 距离 / 速度 × 60（转换为分钟）
        double travelTimeMinutes = (ROAD_LENGTH / averageSpeed) * 60;
        return travelTimeMinutes;
    }

    /**
     * 计算等待时间：速度越低等待时间越长
     */
    private double calculateWaitingTime(double averageSpeed) {
        // 速度比：实际速度 / 自由流速度
        double speedRatio = averageSpeed / FREE_FLOW_SPEED;

        if (speedRatio >= 0.8) {
            // 畅通：几乎无等待
            return ThreadLocalRandom.current().nextDouble(0.0, 0.5);
        } else if (speedRatio >= 0.5) {
            // 轻度拥堵
            return ThreadLocalRandom.current().nextDouble(0.5, 2.0);
        } else if (speedRatio >= 0.3) {
            // 中度拥堵
            return ThreadLocalRandom.current().nextDouble(2.0, 5.0);
        } else {
            // 严重拥堵
            return ThreadLocalRandom.current().nextDouble(5.0, 15.0);
        }
    }

    /**
     * 计算时间损失（分钟）
     */
    private double calculateTimeLoss(double averageTravelTime) {
        // 自由流行程时间
        double freeFlowTravelTime = (ROAD_LENGTH / FREE_FLOW_SPEED) * 60;

        // 时间损失 = 实际行程时间 - 自由流行程时间
        double timeLoss = Math.max(0, averageTravelTime - freeFlowTravelTime);

        // 添加微小随机波动
        return timeLoss * ThreadLocalRandom.current().nextDouble(0.9, 1.1);
    }

    /**
     * 计算 CO2 排放总量（kg）
     */
    private double calculateCo2Emission(int vehicleCount, String vehicleType, double totalRouteLength) {
        // 获取车辆类型的 CO2 排放系数（g/km）
        double emissionFactor = CO2_EMISSION_FACTORS.getOrDefault(vehicleType, 0.12);

        // 总排放量（g）= 车辆数 × 行驶距离（km）× 排放系数（g/km）
        double totalEmissionGrams = vehicleCount * (totalRouteLength / 1000.0) * emissionFactor;

        // 转换为 kg
        double totalEmissionKg = totalEmissionGrams / 1000.0;

        // 添加随机波动 ±10%
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.9, 1.1);

        return totalEmissionKg * randomFactor;
    }

    /**
     * 四舍五入保留指定小数位
     */
    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }
}