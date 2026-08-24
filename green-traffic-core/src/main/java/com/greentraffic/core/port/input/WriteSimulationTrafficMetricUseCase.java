package com.greentraffic.core.port.input;

/**
 * 写入 SUMO 仿真指标的输入端口。
 */
public interface WriteSimulationTrafficMetricUseCase {

    void write(WriteSimulationTrafficMetricCommand command);
}