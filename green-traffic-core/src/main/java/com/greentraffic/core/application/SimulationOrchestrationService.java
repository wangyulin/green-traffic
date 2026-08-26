package com.greentraffic.core.application;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.input.SimulationOrchestrationUseCase;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.common.util.TimezoneUtils;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 将仿真计算逻辑迁移到 core 的实现，负责生成仿真数据并发布消息。
 */
public class SimulationOrchestrationService implements SimulationOrchestrationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MessagePublisher messagePublisher;

    // 模拟参数
    private static final double FREE_FLOW_SPEED = 60.0;
    private static final double JAM_DENSITY = 2000.0;
    private static final double ROAD_LENGTH = 15.0;

    private static final Map<String, Double> CO2_EMISSION_FACTORS = Map.of(
            "passenger", 0.12,
            "truck", 0.35,
            "bus", 0.25,
            "motorcycle", 0.06
    );

    private static final String[] VEHICLE_TYPES = {"passenger", "passenger", "passenger", "truck", "bus", "motorcycle"};
    private static final String[] DIRECTIONS = {"NORTH", "SOUTH", "EAST", "WEST", "UNKNOWN"};
    private static final String ROAD_ID = "Carbon-GRID";

    public SimulationOrchestrationService(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
        logger.info("SimulationOrchestrationService initialized");
    }

    @Override
    public void generateAndPublish() {
        logger.info("Simulation orchestration triggered");
        SimulationTrafficMetric metric = generateTrafficMetric();
        Message<SimulationTrafficMetric> msg = Message.of(TrafficMessageTypes.CO2_EMISSION, metric);
        try {
            messagePublisher.publishAsync(msg);
        } catch (Exception ex) {
            logger.warn("publishAsync failed, falling back to publish(): {}", ex.getMessage());
            messagePublisher.publish(msg);
        }
        logger.debug("Published simulated metric via core use-case: {}", msg.getMessageId());
    }

    private SimulationTrafficMetric generateTrafficMetric() {
        String simulationId = UUID.randomUUID().toString();
        String vehicleType = VEHICLE_TYPES[ThreadLocalRandom.current().nextInt(VEHICLE_TYPES.length)];
        String direction = DIRECTIONS[ThreadLocalRandom.current().nextInt(DIRECTIONS.length)];

        int vehicleCount = generateVehicleCount();
        double averageSpeed = generateAverageSpeed(vehicleCount);
        double averageTravelTime = calculateTravelTime(averageSpeed);
        double averageWaitingTime = calculateWaitingTime(averageSpeed);
        double averageTimeLoss = calculateTimeLoss(averageTravelTime);
        double totalRouteLength = vehicleCount * ROAD_LENGTH * 1000;
        double totalCo2Emission = calculateCo2Emission(vehicleCount, vehicleType, totalRouteLength);
        Instant ts = TimezoneUtils.normalizeInstant(Instant.now());

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

    private int generateVehicleCount() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        double peakFactor = getPeakFactor(currentTime, dayOfWeek);
        int baseFlow = 500;
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.8, 1.2);
        int vehicleCount = (int) Math.round(baseFlow * peakFactor * randomFactor);
        return Math.max(50, Math.min(2000, vehicleCount));
    }

    private double getPeakFactor(LocalTime time, DayOfWeek dayOfWeek) {
        int hour = time.getHour();
        int minute = time.getMinute();
        double timeInHours = hour + minute / 60.0;
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
        if (isWeekend) {
            if (timeInHours >= 10 && timeInHours < 14) return 1.2;
            else if (timeInHours >= 16 && timeInHours < 19) return 1.3;
            else if (timeInHours >= 22 || timeInHours < 6) return 0.3;
            else return 0.8;
        } else {
            if (timeInHours >= 7 && timeInHours < 9) return 1.8;
            else if (timeInHours >= 17 && timeInHours < 19) return 2.0;
            else if (timeInHours >= 9 && timeInHours < 17) return 1.0;
            else if (timeInHours >= 19 && timeInHours < 22) return 1.4;
            else return 0.2;
        }
    }

    private double generateAverageSpeed(int vehicleCount) {
        double capacity = 1500.0;
        double vcRatio = vehicleCount / capacity;
        double travelTimeFactor = 1 + 0.15 * Math.pow(vcRatio, 4);
        double baseSpeed = FREE_FLOW_SPEED / travelTimeFactor;
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
        double speed = baseSpeed * randomFactor;
        return Math.max(5.0, Math.min(FREE_FLOW_SPEED, speed));
    }

    private double calculateTravelTime(double averageSpeed) {
        double travelTimeMinutes = (ROAD_LENGTH / averageSpeed) * 60;
        return travelTimeMinutes;
    }

    private double calculateWaitingTime(double averageSpeed) {
        double speedRatio = averageSpeed / FREE_FLOW_SPEED;
        if (speedRatio >= 0.8) return ThreadLocalRandom.current().nextDouble(0.0, 0.5);
        else if (speedRatio >= 0.5) return ThreadLocalRandom.current().nextDouble(0.5, 2.0);
        else if (speedRatio >= 0.3) return ThreadLocalRandom.current().nextDouble(2.0, 5.0);
        else return ThreadLocalRandom.current().nextDouble(5.0, 15.0);
    }

    private double calculateTimeLoss(double averageTravelTime) {
        double freeFlowTravelTime = (ROAD_LENGTH / FREE_FLOW_SPEED) * 60;
        double timeLoss = Math.max(0, averageTravelTime - freeFlowTravelTime);
        return timeLoss * ThreadLocalRandom.current().nextDouble(0.9, 1.1);
    }

    private double calculateCo2Emission(int vehicleCount, String vehicleType, double totalRouteLength) {
        double emissionFactor = CO2_EMISSION_FACTORS.getOrDefault(vehicleType, 0.12);
        double totalEmissionGrams = vehicleCount * (totalRouteLength / 1000.0) * emissionFactor;
        double totalEmissionKg = totalEmissionGrams / 1000.0;
        double randomFactor = ThreadLocalRandom.current().nextDouble(0.9, 1.1);
        return totalEmissionKg * randomFactor;
    }

    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }
}
