package com.greentraffic.core.domain.simulation;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTrafficGeneratorTest {

    @Test
    void generateProducesValidMetric() {
        java.time.Clock fixedClock = java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T08:00:00Z"), java.time.ZoneOffset.UTC);
        com.greentraffic.core.port.util.IdGenerator idGen = () -> "test-sim-id";
        com.greentraffic.core.port.util.RandomProvider randomProvider = new com.greentraffic.core.port.util.RandomProvider() {
            @Override
            public int nextInt(int bound) { return Math.max(1, bound / 2); }
            @Override
            public double nextDouble(double origin, double bound) { return (origin + bound) / 2.0; }
        };

        SimulationTrafficGenerator generator = new SimulationTrafficGenerator(fixedClock, idGen, randomProvider);
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
