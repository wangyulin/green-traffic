package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.port.output.metrics.SimulationMetricPoint;
import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.infrastructure.config.MetricsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 将 SUMO 仿真指标以 Influx line protocol 写入 VictoriaMetrics。
 */
@Component
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "victoria-metrics")
public class VictoriaSimulationMetricAdapter implements SimulationMetricWritePort {

    private static final String MEASUREMENT = "sumo_traffic_metric";

    private final MetricsProperties properties;
    private final RestTemplate restTemplate;
    private final BlockingQueue<SimulationMetricPoint> queue = new LinkedBlockingQueue<>();
    private ScheduledExecutorService scheduler;

    @Autowired
    public VictoriaSimulationMetricAdapter(MetricsProperties properties) {
        this(properties, new RestTemplate());
    }

    VictoriaSimulationMetricAdapter(MetricsProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::flush,
                properties.getFlushIntervalMs(), properties.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(List<SimulationMetricPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        queue.addAll(points);
        if (queue.size() >= Math.max(1, properties.getBatchSize())) {
            flush();
        }
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        flush();
    }

    synchronized void flush() {
        List<SimulationMetricPoint> drained = new ArrayList<>();
        queue.drainTo(drained);
        if (drained.isEmpty()) {
            return;
        }
        String writeUrl = properties.getVmUrl();
        if (!isAbsoluteUrl(writeUrl)) {
            throw new IllegalStateException("metrics.vmUrl must be an absolute URL for VictoriaMetrics writes");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        int attempt = 0;
        while (attempt <= properties.getMaxRetries()) {
            try {
                restTemplate.postForEntity(writeUrl, new HttpEntity<>(toLineProtocol(drained), headers), String.class);
                return;
            } catch (RuntimeException exception) {
                attempt++;
                if (attempt > properties.getMaxRetries()) {
                    throw exception;
                }
                try {
                    Thread.sleep(properties.getRetryInitialDelayMs() * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("VictoriaMetrics simulation metric write interrupted", interruptedException);
                }
            }
        }
    }

    String toLineProtocol(List<SimulationMetricPoint> points) {
        StringBuilder payload = new StringBuilder();
        for (SimulationMetricPoint point : points) {
            payload.append(MEASUREMENT);
            addTag(payload, "simulationId", point.simulationId());
            addTag(payload, "roadId", point.roadId());
            addTag(payload, "direction", point.direction());
            addTag(payload, "vehicleType", point.vehicleType());
            payload.append(' ')
                    .append("vehicleCount=").append(point.vehicleCount()).append('i')
                    .append(",averageSpeed=").append(point.averageSpeed())
                    .append(",totalCo2Emission=").append(point.totalCo2Emission())
                    .append(",averageTravelTime=").append(point.averageTravelTime())
                    .append(",averageWaitingTime=").append(point.averageWaitingTime())
                    .append(",averageTimeLoss=").append(point.averageTimeLoss())
                    .append(",totalRouteLength=").append(point.totalRouteLength())
                    .append(' ').append(toNanoseconds(point.timestamp())).append('\n');
        }
        return payload.toString();
    }

    private void addTag(StringBuilder payload, String name, String value) {
        if (value != null) {
            payload.append(',').append(name).append('=').append(escapeTag(value));
        }
    }

    private long toNanoseconds(Instant timestamp) {
        Instant value = timestamp == null ? Instant.now() : timestamp;
        return value.getEpochSecond() * 1_000_000_000L + value.getNano();
    }

    private boolean isAbsoluteUrl(String url) {
        try {
            return url != null && new URI(url).isAbsolute();
        } catch (Exception exception) {
            return false;
        }
    }

    private String escapeTag(String value) {
        return value.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }
}