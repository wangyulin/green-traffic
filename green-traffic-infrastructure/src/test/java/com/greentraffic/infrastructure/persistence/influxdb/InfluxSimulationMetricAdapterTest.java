package com.greentraffic.infrastructure.persistence.influxdb;

import com.greentraffic.infrastructure.persistence.influxdb.adapter.InfluxSimulationMetricAdapter;
import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

class InfluxSimulationMetricAdapterTest {

    @Test
    void writesSimulationPointsViaInfluxClient() {
        InfluxDbClientProvider provider = mock(InfluxDbClientProvider.class);
        InfluxDBClient client = mock(InfluxDBClient.class);
        WriteApiBlocking writeApi = mock(WriteApiBlocking.class);
        when(client.getWriteApiBlocking()).thenReturn(writeApi);
        when(provider.getClient()).thenReturn(client);

        InfluxDbProperties props = new InfluxDbProperties();
        props.setBucket("test-bucket");
        props.setOrg("test-org");

        InfluxSimulationMetricAdapter adapter = new InfluxSimulationMetricAdapter(provider, props);

        SimulationTrafficMetric metric = new SimulationTrafficMetric(
                "sim-1", "road-1", "EAST", "CAR",
                10, 30.5, 2.5, 12.0, 1.0, 0.5, 1000.0,
                Instant.parse("2026-08-26T10:00:00Z")
        );

        adapter.write(List.of(metric));

        verify(writeApi).writePoints(eq(props.getBucket()), eq(props.getOrg()), anyList());
    }
}
