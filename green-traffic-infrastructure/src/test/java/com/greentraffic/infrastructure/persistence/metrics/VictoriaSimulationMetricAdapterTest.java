package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.port.output.metrics.SimulationMetricPoint;
import com.greentraffic.infrastructure.config.MetricsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VictoriaSimulationMetricAdapterTest {

    @Test
    void serializesSimulationMetricsAsVictoriaCompatibleLineProtocol() {
        MetricsProperties properties = new MetricsProperties();
        VictoriaSimulationMetricAdapter adapter = new VictoriaSimulationMetricAdapter(properties, new RestTemplate());
        SimulationMetricPoint point = new SimulationMetricPoint(
                "sim-1", "SUMO GRID", "UNKNOWN", "passenger", 2,
                45.0, 0.08, 15.0, 3.0, 4.0, 400.0,
                Instant.parse("2026-08-23T08:00:00Z"));

        String payload = adapter.toLineProtocol(List.of(point));

        assertThat(payload).isEqualTo("sumo_traffic_metric,simulationId=sim-1,roadId=SUMO\\ GRID,direction=UNKNOWN,vehicleType=passenger "
                + "vehicleCount=2i,averageSpeed=45.0,totalCo2Emission=0.08,averageTravelTime=15.0,averageWaitingTime=3.0,averageTimeLoss=4.0,totalRouteLength=400.0 1787472000000000000\n");
    }
}