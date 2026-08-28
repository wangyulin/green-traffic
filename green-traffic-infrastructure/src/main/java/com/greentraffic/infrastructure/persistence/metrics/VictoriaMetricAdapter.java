package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;
import com.greentraffic.infrastructure.config.MetricsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "victoria-metrics")
public class VictoriaMetricAdapter implements TrafficMetricStore {
    private static final Logger log = LoggerFactory.getLogger(VictoriaMetricAdapter.class);

    private final MetricsProperties props;
    private final RestTemplate rest;
    private final MetricWriteBuffer buffer;
    private final RetryPolicy retryPolicy;
    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;
    private final AtomicLong writeSuccess = new AtomicLong(0);
    private final AtomicLong writeFailures = new AtomicLong(0);
    private final AtomicLong writeRetries = new AtomicLong(0);
    private ScheduledFuture<?> scheduledTask;

    // primary constructor used in production bootstrap
    @Autowired
    public VictoriaMetricAdapter(MetricsProperties props, RestTemplate rest, @org.springframework.beans.factory.annotation.Qualifier("vmMetricsScheduler") ScheduledExecutorService scheduler) {
        this.props = props;
        this.rest = rest;
        this.scheduler = scheduler;
        this.ownsScheduler = false;
        this.buffer = new InMemoryMetricWriteBuffer(props.getBufferCapacity());
        this.retryPolicy = new ExponentialBackoffRetryPolicy(props.getMaxRetries(), props.getRetryInitialDelayMs());
    }

    // compatibility constructor for tests
    public VictoriaMetricAdapter(MetricsProperties props, RestTemplate rest) {
        this.props = props;
        this.rest = rest;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "vm-metrics-flusher-test");
            t.setDaemon(true);
            return t;
        });
        this.buffer = new InMemoryMetricWriteBuffer(props.getBufferCapacity());
        this.retryPolicy = new ExponentialBackoffRetryPolicy(props.getMaxRetries(), props.getRetryInitialDelayMs());
        this.ownsScheduler = true;
    }

    // remove single-arg constructor to avoid creating RestTemplate inside adapter; prefer injection

    @PostConstruct
    public void start() {
        log.info("[VMAdapter] initialized (vmUrl={})", props.getVmUrl());
        if (props.getVmUrl() == null || props.getVmUrl().isBlank()) {
            log.error("[VMAdapter] metrics.vmUrl is not configured; VictoriaMetrics adapter will not be able to write metrics");
        }
        this.scheduledTask = scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(), props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        flush();
        // persist any remaining buffered points to fallback file if configured
        try {
            int pending = buffer.size();
            if (pending > 0 && props.getFallbackFilePath() != null && !props.getFallbackFilePath().isBlank()) {
                List<TrafficMetric> remaining = buffer.drain(Math.max(1, pending));
                if (!remaining.isEmpty()) {
                    String payload = toLineProtocol(remaining);
                    java.nio.file.Files.writeString(java.nio.file.Path.of(props.getFallbackFilePath()), payload, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    log.info("[VMAdapter] persisted {} pending points to fallback file on shutdown", remaining.size());
                }
            }
        } catch (Exception e) {
            log.warn("[VMAdapter] failed to persist pending points on shutdown: {}", e.getMessage());
        }
        if (ownsScheduler) {
            try {
                scheduler.shutdownNow();
            } catch (Exception e) {
                log.warn("[VMAdapter] error shutting down internal scheduler", e);
            }
        }
        log.info("[VMAdapter] stopped - {}", metricsSummary());
    }

    @Override
    public void write(List<TrafficMetric> points) {
        if (points == null || points.isEmpty()) return;
        for (TrafficMetric p : points) {
            boolean accepted = buffer.offer(p);
            if (!accepted) {
                // buffer full - apply backpressure/fallback policy
                String fallback = props.getFallbackFilePath();
                if (fallback != null && !fallback.isBlank()) {
                    try {
                        String single = toLineProtocol(List.of(p));
                        java.nio.file.Files.writeString(java.nio.file.Path.of(fallback), single, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                        log.warn("[VMAdapter] buffer full - wrote single point to fallback file: {}", fallback);
                        writeFailures.incrementAndGet();
                        continue;
                    } catch (Exception e) {
                        log.error("[VMAdapter] failed to write fallback single point: {}", e.getMessage());
                    }
                }

                if (props.isDropOnFailure()) {
                    log.warn("[VMAdapter] buffer full - dropping point: {}", p);
                    writeFailures.incrementAndGet();
                    continue;
                }

                // trigger a flush and attempt once more
                scheduler.execute(this::flush);
                boolean retried = buffer.offer(p);
                if (!retried) {
                    log.error("[VMAdapter] buffer still full after flush trigger, dropping point");
                    writeFailures.incrementAndGet();
                }
            }
        }
        if (buffer.size() >= Math.max(1, props.getBatchSize())) {
            scheduler.execute(this::flush);
        }
    }

    @Override
    public List<TrafficMetric> query(TrafficMetricQuery query) {
        String queryUrl = props.getVmQueryUrl();

        if (queryUrl == null || queryUrl.isBlank()) {
            throw new IllegalStateException("metrics.vmQueryUrl must be configured for VictoriaMetrics queries");
        }

        String promql = buildPromQl();

        try {
            String url = queryUrl
                    + "?query="
                    + java.net.URLEncoder.encode(promql, StandardCharsets.UTF_8)
                    + "&start=" + query.from().getEpochSecond()
                    + "&end=" + query.to().getEpochSecond()
                    + "&step=1m";

            JsonNode result = rest.getForObject(url, JsonNode.class);
            return toMetricPoints(result);

        } catch (Exception exception) {
            throw new IllegalStateException("VictoriaMetrics metric query failed", exception);
        }
    }

    List<TrafficMetric> toMetricPoints(JsonNode response) {
        List<TrafficMetric> points = new ArrayList<>();
        if (response == null || !"success".equals(response.path("status").asText())) {
            return points;
        }

        class MutablePoint {
            String roadId;
            String direction;
            String vehicleType;
            Integer trafficFlow;
            Double averageSpeed;
            Double co2Emission;
            String location;
            Instant timestamp;
        }

        Map<String, MutablePoint> aggregated = new java.util.HashMap<>();

        for (JsonNode series : response.path("data").path("result")) {
            JsonNode labels = series.path("metric");
            String metricName = labels.path("__name__").asText(null);
            for (JsonNode sample : series.path("values")) {
                if (sample.size() != 2) continue;
                long epoch = sample.get(0).asLong();
                String key = String.join("|",
                        labels.path("roadId").asText(""),
                        labels.path("direction").asText(""),
                        labels.path("vehicleType").asText(""),
                        labels.path("location").asText(""),
                        String.valueOf(epoch)
                );
                MutablePoint mp = aggregated.computeIfAbsent(key, k -> {
                    MutablePoint m = new MutablePoint();
                    m.roadId = labels.path("roadId").asText(null);
                    m.direction = labels.path("direction").asText(null);
                    m.vehicleType = labels.path("vehicleType").asText(null);
                    m.location = labels.path("location").asText(null);
                    m.timestamp = Instant.ofEpochSecond(epoch);
                    return m;
                });

                String valueText = sample.get(1).asText(null);
                if (metricName != null && metricName.contains("averageSpeed")) {
                    try { mp.averageSpeed = Double.parseDouble(valueText); } catch (Exception e) { /* ignore */ }
                } else if (metricName != null && (metricName.contains("co2") || metricName.contains("totalCo2"))) {
                    try { mp.co2Emission = Double.parseDouble(valueText); } catch (Exception e) { /* ignore */ }
                } else if (metricName != null && metricName.contains("trafficFlow")) {
                    try { mp.trafficFlow = (int) Double.parseDouble(valueText); } catch (Exception e) { /* ignore */ }
                } else {
                    try { mp.trafficFlow = (int) Double.parseDouble(valueText); } catch (Exception e) { /* ignore */ }
                }
            }
        }

        for (MutablePoint mp : aggregated.values()) {
            points.add(new TrafficMetric(
                    mp.roadId,
                    mp.direction,
                    mp.vehicleType,
                    mp.trafficFlow,
                    mp.averageSpeed,
                    mp.co2Emission,
                    mp.location,
                    mp.timestamp
            ));
        }

        return points;
    }

    private Integer parseInteger(String value) { try { return (int) Double.parseDouble(value); } catch (NumberFormatException exception) { return null; } }

    private String buildPromQl() { return "traffic_metric"; }

    synchronized void flush() {
        List<TrafficMetric> drained = List.of();
        try {
            int batchSize = Math.max(1, props.getBatchSize());
            drained = buffer.drain(batchSize);
            if (drained.isEmpty()) return;
            String payload = toLineProtocol(drained);
            String url = props.getVmUrl();
            if (url == null || url.isBlank() || !isAbsoluteUrl(url)) {
                log.error("[VMAdapter] invalid or missing metrics.vmUrl configuration: {} - aborting write", url);
                buffer.requeueAtFront(drained);
                return;
            }
            attemptWrite(drained, url, payload);
        } catch (Exception ex) {
            log.error("[VMAdapter] unexpected flush error", ex);
            buffer.requeueAtFront(drained);
        }
    }

    int pendingPointCount() { return buffer.size(); }

    private void requeueAtFront(List<TrafficMetric> points) { buffer.requeueAtFront(points); }

    private boolean isAbsoluteUrl(String url) { try { URI u = new URI(url); return u.isAbsolute(); } catch (Exception e) { return false; } }

    private String toLineProtocol(List<TrafficMetric> points) {
        StringBuilder sb = new StringBuilder();
        for (TrafficMetric p : points) {
            sb.append("traffic_metric,source=simulator");
            if (p.roadId() != null) sb.append(",roadId=").append(escapeTag(p.roadId()));
            if (p.direction() != null) sb.append(",direction=").append(escapeTag(p.direction()));
            if (p.vehicleType() != null) sb.append(",vehicleType=").append(escapeTag(p.vehicleType()));
            if (p.location() != null) sb.append(",location=").append(escapeTag(p.location()));
            sb.append(' ');
            boolean first = true;
            if (p.trafficFlow() != null) { sb.append("trafficFlow=").append(p.trafficFlow()).append('i'); first = false; }
            if (p.averageSpeed() != null) { if (!first) sb.append(','); sb.append("averageSpeed=").append(p.averageSpeed()); first = false; }
            if (p.co2Emission() != null) { if (!first) sb.append(','); sb.append("co2Emission=").append(p.co2Emission()); }
            Instant ts = p.timestamp() == null ? Instant.now() : p.timestamp();
            long nanos = ts.getEpochSecond() * 1_000_000_000L + ts.getNano();
            sb.append(' ').append(nanos).append('\n');
        }
        return sb.toString();
    }

    private void attemptWrite(List<TrafficMetric> drained, String url, String payload) {
        Runnable writeTask = new Runnable() {
            private int attempts = 0;

            @Override
            public void run() {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    if ("bearer".equalsIgnoreCase(props.getAuthType()) && props.getToken() != null) {
                        headers.set("Authorization", "Bearer " + props.getToken());
                    } else if ("basic".equalsIgnoreCase(props.getAuthType()) && props.getUsername() != null) {
                        String cred = props.getUsername() + ":" + (props.getPassword() == null ? "" : props.getPassword());
                        String enc = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
                        headers.set("Authorization", "Basic " + enc);
                    }
                    HttpEntity<String> entity = new HttpEntity<>(payload, headers);
                    ResponseEntity<String> resp = rest.postForEntity(url, entity, String.class);
                    log.debug("[VMAdapter] write response: {}", resp.getStatusCode());
                    if (resp.getStatusCode() == null || !resp.getStatusCode().is2xxSuccessful()) {
                        writeFailures.incrementAndGet();
                        throw new RuntimeException("non-2xx response: " + resp.getStatusCode() + " body:" + resp.getBody());
                    }
                    // success
                    writeSuccess.incrementAndGet();
                } catch (Exception ex) {
                    attempts++;
                    writeRetries.incrementAndGet();
                    if (!retryPolicy.shouldRetry(attempts)) {
                        log.error("[VMAdapter] exhausted retries writing {} points", drained.size());
                        // fallback behavior: write to local fallback file, drop, or requeue depending on config
                        String fallback = props.getFallbackFilePath();
                        if (fallback != null && !fallback.isBlank()) {
                            try {
                                java.nio.file.Files.writeString(
                                        java.nio.file.Path.of(fallback),
                                        payload,
                                        java.nio.file.StandardOpenOption.CREATE,
                                        java.nio.file.StandardOpenOption.APPEND
                                );
                                log.info("[VMAdapter] wrote failed payload to fallback file: {}", fallback);
                                writeFailures.incrementAndGet();
                                return;
                            } catch (Exception e) {
                                log.error("[VMAdapter] failed to write fallback file {}: {}", fallback, e.getMessage());
                                // fall through to requeue
                            }
                        }

                        if (props.isDropOnFailure()) {
                            log.warn("[VMAdapter] dropping {} points after exhausted retries", drained.size());
                            writeFailures.incrementAndGet();
                            return;
                        }

                        buffer.requeueAtFront(drained);
                        writeFailures.incrementAndGet();
                        return;
                    }
                    long delay = retryPolicy.nextDelayMs(attempts);
                    log.warn("[VMAdapter] write attempt {} failed, will retry after {} ms", attempts, delay, ex);
                    scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
                }
            }
        };

        // initial attempt synchronously
        writeTask.run();
    }

    public String metricsSummary() {
        return String.format("VMAdapter[pending=%d,success=%d,fails=%d,retries=%d]",
                pendingPointCount(), writeSuccess.get(), writeFailures.get(), writeRetries.get());
    }

    private String escapeTag(String v) { return v.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\="); }
}
