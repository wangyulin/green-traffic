package com.greentraffic.common.repository;

import com.greentraffic.model.entity.traffic.TrafficMetric;

import java.time.Instant;
import java.util.List;

/**
 * 交通数据存储接口
 * 定义在 common 模块，供 core 和 infrastructure 使用
 */
public interface TrafficDataRepository {

    /**
     * 保存交通数据
     */
    boolean save(TrafficMetric data);

    /**
     * 批量保存交通数据
     */
    boolean saveBatch(List<TrafficMetric> dataList);

    /**
     * 查询指定道路的交通数据
     */
    List<TrafficMetric> findByRoadId(String roadId,
                                          Instant startTime,
                                          Instant endTime);

    /**
     * 查询平均碳排放
     */
    Double findAverageCo2Emission(String roadId,
                                  Instant startTime,
                                  Instant endTime);

    /**
     * 检查连接状态
     */
    boolean isAvailable();

    /**
     * 清理过期数据
     */
    void cleanOldData(Instant beforeTime);
}