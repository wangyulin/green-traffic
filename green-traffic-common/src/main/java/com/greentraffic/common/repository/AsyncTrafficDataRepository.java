package com.greentraffic.common.repository;

import com.greentraffic.model.entity.traffic.TrafficMetric;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步交通数据存储接口
 * 定义在 common 模块
 */
public interface AsyncTrafficDataRepository {

    /**
     * 异步查询交通数据
     */
    CompletableFuture<List<TrafficMetric>> asyncFindByRoadId(
            String roadId,
            Instant startTime,
            Instant endTime
    );

    /**
     * 异步查询平均碳排放
     */
    CompletableFuture<Double> asyncFindAverageCo2Emission(
            String roadId,
            Instant startTime,
            Instant endTime
    );
}