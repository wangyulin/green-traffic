/*
package com.greentraffic.core.handler;

import com.greentraffic.common.messaging.Message;
import com.greentraffic.common.messaging.MessageSubscriber;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.common.messaging.TrafficMessageTypes;
import com.greentraffic.core.port.output.TrafficDataRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

*/
/**
 * 交通数据消息处理器
 * 依赖接口而不是具体实现
 *
 * 模块：green-traffic-core
 * 包：com.greentraffic.core.handler
 *//*

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficDataHandler {

    private final MessageSubscriber messageSubscriber;
    private final TrafficDataRepository trafficDataRepository;  // 依赖接口

    // 统计信息
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        messageSubscriber.subscribe(
                TrafficMessageTypes.TRAFFIC_DATA,
                this::handleTrafficData
        );

        log.info("TrafficDataHandler 已初始化");
    }

    private void handleTrafficData(Message<?> message) {
        try {
            if (message.getPayload() instanceof TrafficMetric data) {
                processTrafficData(data);
                processedCount.incrementAndGet();
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("处理交通数据失败", e);
        }
    }

    private void processTrafficData(TrafficMetric data) {
        // 使用接口保存数据
        boolean success = trafficDataRepository.save(data);

        if (success) {
            log.debug("数据已保存: RoadId={}", data.roadId());
            checkAndSendAlert(data);
        } else {
            log.error("数据保存失败: RoadId={}", data.roadId());
        }
    }

    private void checkAndSendAlert(TrafficMetric data) {
        if (data.co2Emission() != null && data.co2Emission() > 80) {
            log.warn("CO2 排放超标: RoadId={}, CO2={}",
                    data.roadId(), data.co2Emission());
        }
    }

    @PreDestroy
    public void cleanup() {
        messageSubscriber.unsubscribe(TrafficMessageTypes.TRAFFIC_DATA);
        log.info("TrafficDataHandler 已清理");
    }
}*/
