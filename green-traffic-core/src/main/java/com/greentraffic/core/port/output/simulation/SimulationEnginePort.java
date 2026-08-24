package com.greentraffic.core.port.output.simulation;

import java.util.List;

/**
 * 对 SUMO 运行环境的输出端口。
 */
public interface SimulationEnginePort {

    List<SumoTripInfo> run(SumoSimulationRequest request);

    // void stop(String simulationId);

    // SimulationStatus status(String simulationId);

}