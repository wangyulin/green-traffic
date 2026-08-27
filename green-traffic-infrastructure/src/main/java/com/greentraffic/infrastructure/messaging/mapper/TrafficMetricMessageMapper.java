package com.greentraffic.infrastructure.messaging.mapper;

import com.greentraffic.infrastructure.messaging.dto.TrafficMetricMessageV1;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;

public final class TrafficMetricMessageMapper {

    private TrafficMetricMessageMapper() {}

    public static WriteTrafficMetricCommand toCommand(TrafficMetricMessageV1 dto) {
        if (dto == null) return null;
        return new WriteTrafficMetricCommand(
                dto.getRoadId(),
                dto.getDirection(),
                dto.getVehicleType(),
                dto.getTrafficFlow(),
                dto.getAverageSpeed(),
                dto.getCo2Emission(),
                dto.getLocation(),
                dto.getTimestamp()
        );
    }
}
