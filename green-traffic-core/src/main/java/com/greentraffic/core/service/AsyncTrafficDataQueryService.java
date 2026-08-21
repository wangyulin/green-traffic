package com.greentraffic.core.service;

import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.common.repository.AsyncTrafficDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步交通数据查询服务
 * 依赖异步接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTrafficDataQueryService {

    private final AsyncTrafficDataRepository asyncTrafficDataRepository;

    /**
     * 异步查询道路历史数据
     */
    public CompletableFuture<List<TrafficMetric>> asyncQueryRoadHistory(
            String roadId,
            Instant startTime,
            Instant endTime) {

        log.info("异步查询道路历史数据: RoadId={}", roadId);
        return asyncTrafficDataRepository.asyncFindByRoadId(roadId, startTime, endTime);
    }

    /**
     * 异步查询平均碳排放
     */
    public CompletableFuture<Double> asyncQueryAverageCo2(
            String roadId,
            Instant startTime,
            Instant endTime) {

        log.info("异步查询平均碳排放: RoadId={}", roadId);
        return asyncTrafficDataRepository.asyncFindAverageCo2Emission(
                roadId, startTime, endTime
        );
    }
}