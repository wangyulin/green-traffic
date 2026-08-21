package com.greentraffic.core.domain.service;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * 交通数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficDataService {

    private final MessagePublisher messagePublisher;
    private final Random random = new Random();

    /**
     * 生成并发送交通数据
     */
        public void generateAndSendTrafficData() {
        TrafficMetric metric = new TrafficMetric(
            "ROAD_" + random.nextInt(100),
            null,
            getRandomVehicleType(),
            random.nextInt(200) + 50,
            random.nextDouble() * 60 + 10,
            random.nextDouble() * 100,
            "Location_" + random.nextInt(50),
            java.time.Instant.now()
        );

        // 创建消息
        Message<TrafficMetric> message = Message.of(
            TrafficMessageTypes.TRAFFIC_DATA,
            metric
        );

        // 发布消息
        messagePublisher.publish(message);

        log.info("交通数据已发送: {}", metric);
        }

    /**
     * 批量发送交通数据
     */
    public void sendBatchTrafficData(int count) {
        for (int i = 0; i < count; i++) {
            generateAndSendTrafficData();
        }
    }

    private String getRandomVehicleType() {
        String[] types = {"CAR", "TRUCK", "BUS", "MOTORCYCLE"};
        return types[random.nextInt(types.length)];
    }
}
