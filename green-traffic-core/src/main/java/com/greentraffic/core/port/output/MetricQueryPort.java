package com.greentraffic.core.port.output;

import com.greentraffic.core.port.output.metrics.MetricPoint;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;

import java.util.List;

/**
 * 交通指标查询 Port。
 *
 * <p>定义 Core 查询交通指标所需要的外部能力，
 * 不暴露 Map 等泛化查询参数。</p>
 */
public interface MetricQueryPort {

    /**
     * 根据明确的交通指标查询条件查询指标点。
     *
     * @param query 查询条件
     * @return 指标点列表
     */
    List<MetricPoint> query(TrafficMetricQuery query);
}
