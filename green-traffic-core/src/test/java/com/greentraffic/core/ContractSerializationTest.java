package com.greentraffic.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class ContractSerializationTest {

    @Test
    void messageSerializationRoundtrip_preservesFieldsAndTimestamp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimulationTrafficMetric metric = new SimulationTrafficMetric(
                "sim-1",
                "road-1",
                "N",
                "car",
                10,
                35.5,
                1.23,
                120.0,
                30.0,
                5.0,
                1000.0,
                Instant.parse("2026-08-27T00:00:00Z")
        );

        Message<SimulationTrafficMetric> message = Message.of(TrafficMessageTypes.CO2_EMISSION, metric);

        String json = mapper.writeValueAsString(message);

        assertNotNull(json);
        assertTrue(json.contains(TrafficMessageTypes.CO2_EMISSION));
        assertTrue(json.contains("timestamp"));

        Message<SimulationTrafficMetric> read = mapper.readValue(json, new TypeReference<>() {});

        assertNotNull(read);
        assertEquals(message.getMessageType(), read.getMessageType());
        assertEquals(message.getPayload().simulationId(), read.getPayload().simulationId());
        assertEquals(message.getPayload().totalCo2Emission(), read.getPayload().totalCo2Emission());
        assertEquals(message.getTimestamp(), read.getTimestamp());
    }
}
