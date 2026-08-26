package com.greentraffic.core.domain.simulation;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTrafficGeneratorTest {

    @Test
    void generateProducesValidMetric() {
        SimulationTrafficGenerator generator = new SimulationTrafficGenerator();
        SimulationTrafficMetric m = generator.generate();

        assertNotNull(m.simulationId());
        assertNotNull(m.roadId());
        assertNotNull(m.direction());
        assertNotNull(m.vehicleType());
        assertTrue(m.vehicleCount() >= 50 && m.vehicleCount() <= 2000);
        assertTrue(m.averageSpeed() >= 5.0 && m.averageSpeed() <= 60.0);
        assertTrue(m.totalCo2Emission() >= 0.0);
        assertTrue(m.averageTravelTime() >= 0.0);
        assertTrue(m.totalRouteLength() >= 0.0);
        assertNotNull(m.timestamp());
    }
}
