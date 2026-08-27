package com.greentraffic.core.application;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficMetricQueryApplicationServiceTest {

    @Test
    void queriesMetricPortAndMapsPointsToTrafficMetrics() {

        TrafficMetricStore queryPort =
                mock(TrafficMetricStore.class);

        TrafficMetricQueryApplicationService service =
                new TrafficMetricQueryApplicationService(queryPort);

        Instant start =
                Instant.parse("2026-08-22T10:00:00Z");

        Instant end =
                Instant.parse("2026-08-22T11:00:00Z");

                TrafficMetric point =
                        new TrafficMetric(
                                "ROAD-001",
                                "EAST",
                                "CAR",
                                120,
                                42.5,
                                12.3,
                                "Wangjing",
                                start
                        );

        TrafficMetricQuery query =
                new TrafficMetricQuery(start, end);

        when(queryPort.query(query))
                .thenReturn(List.of(point));

        List<TrafficMetric> metrics =
                service.query(start, end);

        assertThat(metrics)
                .containsExactly(
                        new TrafficMetric(
                                "ROAD-001",
                                "EAST",
                                "CAR",
                                120,
                                42.5,
                                12.3,
                                "Wangjing",
                                start
                        )
                );

        verify(queryPort).query(query);
    }
}