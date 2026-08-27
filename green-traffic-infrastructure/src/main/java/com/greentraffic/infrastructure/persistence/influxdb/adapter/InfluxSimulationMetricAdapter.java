package com.greentraffic.infrastructure.persistence.influxdb.adapter;

import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.greentraffic.infrastructure.config.MetricsProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "influx")
public class InfluxSimulationMetricAdapter implements SimulationMetricStore {

    private static final String MEASUREMENT = "sumo_traffic_metric";
    private final InfluxDBClient client;
    private final InfluxDbProperties properties;
    private final MetricsProperties metricsProperties;

    @Autowired
    public InfluxSimulationMetricAdapter(InfluxDbClientProvider clientProvider, InfluxDbProperties properties, MetricsProperties metricsProperties) {
        this.client = clientProvider.getClient();
        this.properties = properties;
        this.metricsProperties = metricsProperties;
    }

    // Backwards-compatible constructor for tests and older callers
    public InfluxSimulationMetricAdapter(InfluxDbClientProvider clientProvider, InfluxDbProperties properties) {
        this(clientProvider, properties, new MetricsProperties());
    }

    @Override
    public void write(List<SimulationTrafficMetric> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        try {
            org.slf4j.LoggerFactory.getLogger(InfluxSimulationMetricAdapter.class).debug("[InfluxSimulationAdapter] writing {} simulation points to bucket={} org={}", points.size(), properties.getBucket(), properties.getOrg());
            client.getWriteApiBlocking().writePoints(properties.getBucket(), properties.getOrg(),
                    points.stream().map(this::toPoint).toList());
            org.slf4j.LoggerFactory.getLogger(InfluxSimulationMetricAdapter.class).debug("[InfluxSimulationAdapter] write completed");
        } catch (Exception e) {
            // fallback behavior: write failed points to local fallback file if configured, else rethrow
            String fallback = metricsProperties.getFallbackFilePath();
            if (fallback != null && !fallback.isBlank()) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (SimulationTrafficMetric p : points) {
                        sb.append(p.simulationId()).append(',').append(p.roadId()).append(',').append(p.vehicleCount()).append(',').append(p.averageSpeed()).append(',').append(p.totalCo2Emission()).append(',').append(p.timestamp()).append('\n');
                    }
                    java.nio.file.Files.writeString(java.nio.file.Path.of(fallback), sb.toString(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    return;
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to write points to Influx and fallback file", ex);
                }
            }
            throw e;
        }
    }

    private Point toPoint(SimulationTrafficMetric metric) {
        Point point = Point.measurement(MEASUREMENT)
                .time(metric.timestamp() == null ? Instant.now() : metric.timestamp(), WritePrecision.NS)
                .addTag("simulationId", metric.simulationId())
                .addTag("source", "sumo")
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

    @PostConstruct
    void init() {
        org.slf4j.LoggerFactory.getLogger(InfluxSimulationMetricAdapter.class).info("[InfluxSimulationAdapter] initialized for bucket={} org={} fallback={}", properties.getBucket(), properties.getOrg(), metricsProperties.getFallbackFilePath());
    }

    private void addField(Point point, String name, Number value) {
        if (value != null) {
            point.addField(name, value);
        }
    }
}