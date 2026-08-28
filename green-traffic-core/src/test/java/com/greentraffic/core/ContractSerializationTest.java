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

    @Test
    void deserialize_missingOptionalFields_shouldSucceed() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // JSON without schemaVersion and headers (older client)
        String legacyJson = "{\"messageType\":\"" + TrafficMessageTypes.CO2_EMISSION + "\",\"payload\":{\"simulationId\":\"sim-legacy\"},\"timestamp\":\"2026-08-27T00:00:00Z\"}";

        Message<?> read = mapper.readValue(legacyJson, new TypeReference<>() {});
        assertNotNull(read);
        assertEquals(TrafficMessageTypes.CO2_EMISSION, read.getMessageType());
        assertEquals("sim-legacy", ((java.util.Map)read.getPayload()).get("simulationId"));
    }

    @Test
    void deserialize_withAdditionalUnknownFields_shouldIgnoreExtras() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String noisyJson = "{\"messageType\":\"" + TrafficMessageTypes.CO2_EMISSION + "\",\"payload\":{\"simulationId\":\"sim-noisy\",\"extra\":123},\"timestamp\":\"2026-08-27T00:00:00Z\",\"unexpected\":true}";

        Message<?> read = mapper.readValue(noisyJson, new TypeReference<>() {});
        assertNotNull(read);
        assertEquals("sim-noisy", ((java.util.Map)read.getPayload()).get("simulationId"));
    }
}
