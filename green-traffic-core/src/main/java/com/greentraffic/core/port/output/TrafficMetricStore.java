package com.greentraffic.core.port.output;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;

import java.util.List;

/**
 * 能力化 Port：交通指标存储（业务能力命名），替代技术导向的 MetricWritePort/MetricQueryPort。
 */
public interface TrafficMetricStore {

    void write(List<TrafficMetric> points);

    List<TrafficMetric> query(TrafficMetricQuery query);
}
