package com.greentraffic.core;

import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ContractTest {

    @Test
    void trafficMessageTypesExist() {
        // 简单契约测试：确保核心消息类型常量存在且非空
        assertNotNull(TrafficMessageTypes.TRAFFIC_DATA);
        assertFalse(TrafficMessageTypes.TRAFFIC_DATA.isEmpty());

        assertNotNull(TrafficMessageTypes.CO2_EMISSION);
        assertFalse(TrafficMessageTypes.CO2_EMISSION.isEmpty());
    }
}
