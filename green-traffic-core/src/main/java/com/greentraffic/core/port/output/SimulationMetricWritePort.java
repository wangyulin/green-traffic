package com.greentraffic.core.port.output;

import com.greentraffic.core.port.output.metrics.SimulationMetricPoint;

import java.util.List;

/**
 * SUMO 仿真指标的时序存储输出端口。
 */
public interface SimulationMetricWritePort {

    void write(List<SimulationMetricPoint> points);
}