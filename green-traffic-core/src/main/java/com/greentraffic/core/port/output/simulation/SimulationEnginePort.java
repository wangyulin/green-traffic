package com.greentraffic.core.port.output.simulation;

import java.util.List;

/**
 * 对仿真引擎（SUMO / 外部引擎）的输出端口。
 * <p>
 * 为了避免当前 port 处于“半成品状态”，这里补齐常见操作：
 * - 同步运行 {@link #run(SumoSimulationRequest)}（已存在）
 * - 异步启动 {@link #runAsync(SumoSimulationRequest)}（默认实现以后台线程委托给同步实现）
 * - 停止正在运行的仿真 {@link #stop(String)}（默认不支持）
 * - 查询仿真状态 {@link #status(String)}（返回 {@link SimulationStatus}）
 */
public interface SimulationEnginePort {

    /**
     * 同步运行仿真并返回原始 tripinfo 列表。
     */
    List<SumoTripInfo> run(SumoSimulationRequest request);

    /**
     * 异步运行仿真，返回用于标识此次仿真的 id（通常为 request.simulationId()）。
     * 默认实现会在新线程中调用 {@link #run(SumoSimulationRequest)} 并立即返回 request 的 id。
     */
    default String runAsync(SumoSimulationRequest request) {
        Thread t = new Thread(() -> run(request));
        t.setDaemon(true);
        t.start();
        return request.simulationId();
    }

    /**
     * 停止指定仿真；默认实现不支持并抛出 {@link UnsupportedOperationException}。
     */
    default void stop(String simulationId) {
        throw new UnsupportedOperationException("stop not supported by this SimulationEnginePort");
    }

    /**
     * 查询仿真状态，默认返回 {@link SimulationStatus#UNKNOWN}。
     */
    default SimulationStatus status(String simulationId) {
        return SimulationStatus.UNKNOWN;
    }

}
