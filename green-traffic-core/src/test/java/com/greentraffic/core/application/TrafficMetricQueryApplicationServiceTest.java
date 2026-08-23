package com.greentraffic.core.application;

import com.greentraffic.common.port.metrics.MetricPoint;
import com.greentraffic.common.port.metrics.MetricQueryPort;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficMetricQueryApplicationServiceTest {

    @Test
    void queriesMetricPortAndMapsPointsToTrafficMetrics() {
        MetricQueryPort queryPort = mock(MetricQueryPort.class);
        TrafficMetricQueryApplicationService service = new TrafficMetricQueryApplicationService(queryPort);
        Instant start = Instant.parse("2026-08-22T10:00:00Z");
        Instant end = Instant.parse("2026-08-22T11:00:00Z");
        MetricPoint point = new MetricPoint(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, "Wangjing", start
        );
        when(queryPort.query(start, end, Map.of())).thenReturn(List.of(point));

        List<TrafficMetric> metrics = service.query(start, end);

        assertThat(metrics).containsExactly(new TrafficMetric(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, "Wangjing", start
        ));
        verify(queryPort).query(eq(start), eq(end), eq(Map.of()));
    }
}