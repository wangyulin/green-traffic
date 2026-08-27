package com.greentraffic.core.port.output.simulation;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationEnginePortContractTest {

    @Test
    void defaultRunAsyncDelegatesToRunAndReturnsId() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        var adapter = new SimulationEnginePort() {
            @Override
            public List<SumoTripInfo> run(SumoSimulationRequest request) {
                latch.countDown();
                return List.of();
            }
        };

        SumoSimulationRequest req = new SumoSimulationRequest("sim-1", Path.of("target/sumo-test"), 1, 1);
        String id = adapter.runAsync(req);
        assertEquals("sim-1", id);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "runAsync should delegate to run in background");
    }

    @Test
    void defaultStopThrowsWhenNotImplemented() {
        var adapter = new SimulationEnginePort() {
            @Override
            public List<SumoTripInfo> run(SumoSimulationRequest request) {
                return List.of();
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> adapter.stop("any"));
    }
}
