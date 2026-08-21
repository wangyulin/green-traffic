package com.greentraffic.core.service;

import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.common.repository.TrafficDataRepository;  // 依赖接口，不是具体实现
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    public List<TrafficMetric> queryRoadHistory(String roadId,
                                                     Instant startTime,
                                                     Instant endTime) {
        log.info("查询道路历史数据: RoadId={}, Start={}, End={}",
                roadId, startTime, endTime);

        return trafficDataRepository.findByRoadId(roadId, startTime, endTime);
    }

    /**
     * 查询最近一小时的数据
     */
    public List<TrafficMetric> queryRecentHourData(String roadId) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(3600);

        return queryRoadHistory(roadId, startTime, endTime);
    }

    /**
     * 查询最近一天的数据
     */
    public List<TrafficMetric> queryRecentDayData(String roadId) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(86400);

        return queryRoadHistory(roadId, startTime, endTime);
    }

    /**
     * 查询道路平均碳排放
     */
    public Double queryAverageCo2(String roadId,
                                  Instant startTime,
                                  Instant endTime) {
        log.info("查询道路平均碳排放: RoadId={}", roadId);

        return trafficDataRepository.findAverageCo2Emission(roadId, startTime, endTime);
    }

    /**
     * 查询最近一小时的平均碳排放
     */
    public Double queryRecentHourAverageCo2(String roadId) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(3600);

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
    public void cleanOldData(Instant beforeTime) {
        log.info("清理过期数据: {}", beforeTime);
        trafficDataRepository.cleanOldData(beforeTime);
    }
}