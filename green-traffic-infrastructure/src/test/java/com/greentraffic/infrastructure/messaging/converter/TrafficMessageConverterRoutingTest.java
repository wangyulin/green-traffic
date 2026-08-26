package com.greentraffic.infrastructure.messaging.converter;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.messaging.Message;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficMessageConverterRoutingTest {

    private final TrafficMessageConverter trafficConverter =
            new TrafficMessageConverter();

    private final SimulationTrafficMessageConverter simulationConverter =
            new SimulationTrafficMessageConverter();

    @Test
    void routesOrdinaryCo2PayloadOnlyToTrafficConverter() {
        Message<Map<String, Object>> message = Message.of(
                TrafficMessageTypes.CO2_EMISSION,
                Map.of(
                        "roadId", "ROAD-001",
                        "trafficFlow", 120,
                        "averageSpeed", 42.5
                )
        );

        assertThat(trafficConverter.supports(message)).isTrue();
        assertThat(simulationConverter.supports(message)).isFalse();
    }

    @Test
    void routesSimulationCo2PayloadOnlyToSimulationConverter() {
        Message<Map<String, Object>> message = Message.of(
                TrafficMessageTypes.CO2_EMISSION,
                Map.ofEntries(
                        Map.entry("simulationId", "sim-1"),
                        Map.entry("roadId", "ROAD-001"),
                        Map.entry("direction", "EAST"),
                        Map.entry("vehicleType", "CAR"),
                        Map.entry("vehicleCount", 10),
                        Map.entry("averageSpeed", 40.0),
                        Map.entry("totalCo2Emission", 0.5),
                        Map.entry("averageTravelTime", 5.0),
                        Map.entry("averageWaitingTime", 0.5),
                        Map.entry("averageTimeLoss", 0.2),
                        Map.entry("totalRouteLength", 15000.0),
                        Map.entry("timestamp", "2026-08-22T10:00:00Z")
                )
        );

        assertThat(trafficConverter.supports(message)).isFalse();
        assertThat(simulationConverter.supports(message)).isTrue();

        SimulationTrafficMetric metric = simulationConverter.convert(message).getPayload();
        assertThat(metric.simulationId()).isEqualTo("sim-1");
        assertThat(metric.totalCo2Emission()).isEqualTo(0.5);
    }
}