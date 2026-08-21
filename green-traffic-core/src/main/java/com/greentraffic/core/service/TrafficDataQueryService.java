package com.greentraffic.core.service;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.common.repository.TrafficDataRepository;  // 依赖接口，不是具体实现
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 交通数据查询服务
 * 依赖接口而不是具体实现
 *
 * 模块：green-traffic-core
 * 包：com.greentraffic.core.service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficDataQueryService {

    // 依赖接口，Spring 会自动注入 InfluxDBTrafficDataRepository 实现
    private final TrafficDataRepository trafficDataRepository;

    /**
     * 查询道路历史数据
     */
    public List<TrafficDataMessage> queryRoadHistory(String roadId,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        log.info("查询道路历史数据: RoadId={}, Start={}, End={}",
                roadId, startTime, endTime);

        return trafficDataRepository.findByRoadId(roadId, startTime, endTime);
    }

    /**
     * 查询最近一小时的数据
     */
    public List<TrafficDataMessage> queryRecentHourData(String roadId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        return queryRoadHistory(roadId, startTime, endTime);
    }

    /**
     * 查询最近一天的数据
     */
    public List<TrafficDataMessage> queryRecentDayData(String roadId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(1);

        return queryRoadHistory(roadId, startTime, endTime);
    }

    /**
     * 查询道路平均碳排放
     */
    public Double queryAverageCo2(String roadId,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime) {
        log.info("查询道路平均碳排放: RoadId={}", roadId);

        return trafficDataRepository.findAverageCo2Emission(roadId, startTime, endTime);
    }

    /**
     * 查询最近一小时的平均碳排放
     */
    public Double queryRecentHourAverageCo2(String roadId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        return queryAverageCo2(roadId, startTime, endTime);
    }

    /**
     * 检查数据存储状态
     */
    public boolean isStorageAvailable() {
        boolean available = trafficDataRepository.isAvailable();
        log.info("数据存储状态: {}", available ? "可用" : "不可用");
        return available;
    }

    /**
     * 清理过期数据
     */
    public void cleanOldData(LocalDateTime beforeTime) {
        log.info("清理过期数据: {}", beforeTime);
        trafficDataRepository.cleanOldData(beforeTime);
    }
}