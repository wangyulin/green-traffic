package com.greentraffic.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greentraffic.infrastructure.messaging.dto.TrafficMetricMessageV1;
import com.greentraffic.infrastructure.messaging.mapper.TrafficMetricMessageMapper;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TrafficMetricMessageContractTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    public void dto_serialize_deserialize_preserves_fields() throws Exception {
        TrafficMetricMessageV1 dto = new TrafficMetricMessageV1(
                "R-1",
                "N",
                "car",
                123,
                45.6,
                7.89,
                "loc-1",
                Instant.parse("2026-08-27T05:00:00Z")
        );

        String json = mapper.writeValueAsString(dto);
        TrafficMetricMessageV1 round = mapper.readValue(json, TrafficMetricMessageV1.class);

        assertEquals(dto.getRoadId(), round.getRoadId());
        assertEquals(dto.getDirection(), round.getDirection());
        assertEquals(dto.getVehicleType(), round.getVehicleType());
        assertEquals(dto.getTrafficFlow(), round.getTrafficFlow());
        assertEquals(dto.getAverageSpeed(), round.getAverageSpeed());
        assertEquals(dto.getCo2Emission(), round.getCo2Emission());
        assertEquals(dto.getLocation(), round.getLocation());
        assertEquals(dto.getTimestamp(), round.getTimestamp());
    }

    @Test
    public void mapper_converts_dto_to_command() {
        TrafficMetricMessageV1 dto = new TrafficMetricMessageV1(
                "R-2",
                "S",
                "truck",
                10,
                12.3,
                0.45,
                "loc-2",
                Instant.now()
        );

        WriteTrafficMetricCommand cmd = TrafficMetricMessageMapper.toCommand(dto);
        assertNotNull(cmd);
        assertEquals(dto.getRoadId(), cmd.roadId());
        assertEquals(dto.getDirection(), cmd.direction());
        assertEquals(dto.getVehicleType(), cmd.vehicleType());
        assertEquals(dto.getTrafficFlow(), cmd.trafficFlow());
        assertEquals(dto.getAverageSpeed(), cmd.averageSpeed());
        assertEquals(dto.getCo2Emission(), cmd.co2Emission());
        assertEquals(dto.getLocation(), cmd.location());
        assertEquals(dto.getTimestamp(), cmd.timestamp());
    }
}
