package com.greentraffic.infrastructure.persistence.influxdb.adapter;

import com.greentraffic.core.port.output.metrics.SimulationMetricPoint;
import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "influx")
public class InfluxSimulationMetricAdapter implements SimulationMetricWritePort {

    private static final String MEASUREMENT = "sumo_traffic_metric";
    private final InfluxDBClient client;
    private final InfluxDbProperties properties;

    public InfluxSimulationMetricAdapter(InfluxDbClientProvider clientProvider, InfluxDbProperties properties) {
        this.client = clientProvider.getClient();
        this.properties = properties;
    }

    @Override
    public void write(List<SimulationMetricPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        client.getWriteApiBlocking().writePoints(properties.getBucket(), properties.getOrg(),
                points.stream().map(this::toPoint).toList());
    }

    private Point toPoint(SimulationMetricPoint metric) {
        Point point = Point.measurement(MEASUREMENT)
                .time(metric.timestamp() == null ? Instant.now() : metric.timestamp(), WritePrecision.NS)
                .addTag("simulationId", metric.simulationId())
                .addTag("roadId", metric.roadId())
                .addTag("direction", metric.direction())
                .addTag("vehicleType", metric.vehicleType());
        addField(point, "vehicleCount", metric.vehicleCount());
        addField(point, "averageSpeed", metric.averageSpeed());
        addField(point, "totalCo2Emission", metric.totalCo2Emission());
        addField(point, "averageTravelTime", metric.averageTravelTime());
        addField(point, "averageWaitingTime", metric.averageWaitingTime());
        addField(point, "averageTimeLoss", metric.averageTimeLoss());
        addField(point, "totalRouteLength", metric.totalRouteLength());
        return point;
    }

    private void addField(Point point, String name, Number value) {
        if (value != null) {
            point.addField(name, value);
        }
    }
}