package com.greentraffic.core.port.output.messaging;

/**
 * 交通消息类型定义（位于 core 的 Port 层，作为消息契约）
 */
public final class TrafficMessageTypes {

    private TrafficMessageTypes() {}

    // 交通数据相关
    public static final String TRAFFIC_DATA = "traffic.data";
    public static final String TRAFFIC_DATA_BATCH = "traffic.data.batch";
    public static final String TRAFFIC_ALERT = "traffic.alert";
    public static final String TRAFFIC_MONITOR = "traffic.monitor";

    // 碳排放相关
    public static final String CO2_EMISSION = "co2.emission";
    public static final String CO2_ALERT = "co2.alert";

    // 系统相关
    public static final String SYSTEM_STATUS = "system.status";
    public static final String SYSTEM_METRICS = "system.metrics";
}
