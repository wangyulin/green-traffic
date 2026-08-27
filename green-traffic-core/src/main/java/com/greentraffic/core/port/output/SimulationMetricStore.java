package com.greentraffic.core.port.output;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;

import java.util.List;

/**
 * 能力化 Port：仿真指标存储（业务能力命名），替代技术导向的 SimulationMetricWritePort。
 */
public interface SimulationMetricStore {

    void write(List<SimulationTrafficMetric> points);

}
