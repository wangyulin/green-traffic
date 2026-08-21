package com.greentraffic.common.repository;

import com.greentraffic.common.messaging.AlertMessage;

import java.util.List;

/**
 * 告警数据存储接口
 */
public interface AlertRepository {

    /**
     * 保存告警
     */
    boolean saveAlert(AlertMessage alert);

    /**
     * 查询未处理告警
     */
    List<AlertMessage> findUnprocessedAlerts();

    /**
     * 查询指定道路的告警
     */
    List<AlertMessage> findAlertsByRoadId(String roadId);

    /**
     * 标记告警已处理
     */
    boolean markAlertProcessed(String alertId, String processedBy);

    /**
     * 统计告警数量
     */
    long countAlerts(AlertMessage.AlertLevel level);
}
