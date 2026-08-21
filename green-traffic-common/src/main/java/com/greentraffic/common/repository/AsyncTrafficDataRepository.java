package com.greentraffic.common.repository;

import com.greentraffic.common.messaging.TrafficDataMessage;

import java.time.LocalDateTime;
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
    CompletableFuture<List<TrafficDataMessage>> asyncFindByRoadId(
            String roadId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 异步查询平均碳排放
     */
    CompletableFuture<Double> asyncFindAverageCo2Emission(
            String roadId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}