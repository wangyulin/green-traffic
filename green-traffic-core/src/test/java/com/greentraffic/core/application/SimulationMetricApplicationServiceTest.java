package com.greentraffic.core.application;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SimulationMetricApplicationServiceTest {

    @Test
    void writesAllSimulationFieldsToTheOutputPort() {
        SimulationMetricStore writePort = mock(SimulationMetricStore.class);
        SimulationMetricApplicationService service = new SimulationMetricApplicationService(writePort);
        Instant timestamp = Instant.parse("2026-08-23T08:00:00Z");

        service.write(new WriteSimulationTrafficMetricCommand(
                "sim-1", "edge-1", "EAST", "passenger", 20,
                36.5, 2.4, 42.1, 6.2, 8.3, 1234.5, timestamp));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SimulationTrafficMetric>> captor = ArgumentCaptor.forClass(List.class);
        verify(writePort).write(captor.capture());
        assertThat(captor.getValue()).containsExactly(new SimulationTrafficMetric(
                "sim-1", "edge-1", "EAST", "passenger", 20,
            36.5, 2.4, 42.1, 6.2, 8.3, 1234.5, timestamp));
    }
}