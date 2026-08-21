package com.greentraffic.common.repository;

import com.greentraffic.common.messaging.TrafficDataMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交通数据存储接口
 * 定义在 common 模块，供 core 和 infrastructure 使用
 */
public interface TrafficDataRepository {

    /**
     * 保存交通数据
     */
    boolean save(TrafficDataMessage data);

    /**
     * 批量保存交通数据
     */
    boolean saveBatch(List<TrafficDataMessage> dataList);

    /**
     * 查询指定道路的交通数据
     */
    List<TrafficDataMessage> findByRoadId(String roadId,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime);

    /**
     * 查询平均碳排放
     */
    Double findAverageCo2Emission(String roadId,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime);

    /**
     * 检查连接状态
     */
    boolean isAvailable();

    /**
     * 清理过期数据
     */
    void cleanOldData(LocalDateTime beforeTime);
}