package com.greentraffic.core.service;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessagePublisher;
import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        TrafficDataMessage data = TrafficDataMessage.builder()
                .roadId("ROAD_" + random.nextInt(100))
                .vehicleType(getRandomVehicleType())
                .trafficFlow(random.nextInt(200) + 50)
                .averageSpeed(random.nextDouble() * 60 + 10)
                .co2Emission(random.nextDouble() * 100)
                .timestamp(LocalDateTime.now())
                .location("Location_" + random.nextInt(50))
                .build();

        // 创建消息
        Message<TrafficDataMessage> message = Message.of(
                TrafficMessageTypes.TRAFFIC_DATA,
                data
        );

        // 发布消息
        messagePublisher.publish(message);

        log.info("交通数据已发送: {}", data);
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
