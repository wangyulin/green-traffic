package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.MetricQueryPort;
import com.greentraffic.core.port.output.MetricWritePort;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;
import com.greentraffic.infrastructure.config.MetricsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "victoria-metrics")
public class VictoriaMetricAdapter implements MetricWritePort, MetricQueryPort, TrafficMetricStore {
    private static final Logger log = LoggerFactory.getLogger(VictoriaMetricAdapter.class);

    private final MetricsProperties props;
    private final RestTemplate rest;

    @Autowired
    public VictoriaMetricAdapter(MetricsProperties props) {
        this(props, new RestTemplate());
    }

    public VictoriaMetricAdapter(MetricsProperties props, RestTemplate rest) {
        this.props = props;
        this.rest = rest;
    }
    private final BlockingDeque<TrafficMetric> queue = new LinkedBlockingDeque<>();
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "vm-metrics-flusher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flush, props.getFlushIntervalMs(), props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        flush();
    }

    @Override
    public void write(List<TrafficMetric> points) {
        if (points == null || points.isEmpty()) return;
        for (TrafficMetric p : points) {
            queue.offer(p);
        }
        if (queue.size() >= Math.max(1, props.getBatchSize())) {
            // trigger immediate flush asynchronously
            scheduler.execute(this::flush);
        }
    }

    @Override
    public List<TrafficMetric> query(TrafficMetricQuery query) {

        String queryUrl = props.getVmQueryUrl();

        if (queryUrl == null || queryUrl.isBlank()) {
            throw new IllegalStateException(
                    "metrics.vmQueryUrl must be configured for VictoriaMetrics queries"
            );
        }

        String promql = buildPromQl();

        try {
            String url = queryUrl
                    + "?query="
                    + java.net.URLEncoder.encode(
                    promql,
                    StandardCharsets.UTF_8
            )
                    + "&start="
                    + query.from().getEpochSecond()
                    + "&end="
                    + query.to().getEpochSecond()
                    + "&step=1m";

            JsonNode result =
                    rest.getForObject(url, JsonNode.class);

            return toMetricPoints(result);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "VictoriaMetrics metric query failed",
                    exception
            );
        }
    }

    private List<TrafficMetric> toMetricPoints(JsonNode response) {
        List<TrafficMetric> points = new ArrayList<>();
        if (response == null || !"success".equals(response.path("status").asText())) {
            return points;
        }
        for (JsonNode series : response.path("data").path("result")) {
            JsonNode labels = series.path("metric");
            for (JsonNode sample : series.path("values")) {
                if (sample.size() != 2) {
                    continue;
                }
                points.add(new TrafficMetric(
                    labels.path("roadId").asText(null),
                    labels.path("direction").asText(null),
                    labels.path("vehicleType").asText(null),
                    parseInteger(sample.get(1).asText()),
                    null,
                    null,
                    labels.path("location").asText(null),
                    Instant.ofEpochSecond(sample.get(0).asLong())
                ));
            }
        }
        return points;
    }

    private Integer parseInteger(String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildPromQl() {
        return "traffic_metric";
    }

    synchronized void flush() {
        List<TrafficMetric> drained = List.of();
        try {
            int batchSize = Math.max(1, props.getBatchSize());
            drained = new ArrayList<>(batchSize);
            queue.drainTo(drained, batchSize);
            if (drained.isEmpty()) return;
            String payload = toLineProtocol(drained);
            String url = props.getVmUrl();
            if (url == null || url.isBlank() || !isAbsoluteUrl(url)) {
                log.error("[VMAdapter] invalid or missing metrics.vmUrl configuration: {} - aborting write", url);
                requeueAtFront(drained);
                return;
            }
            int attempts = 0;
            long delay = props.getRetryInitialDelayMs();
            while (attempts <= props.getMaxRetries()) {
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
                    return;
                } catch (Exception ex) {
                    attempts++;
                    log.warn("[VMAdapter] write attempt {} failed, will retry after {} ms", attempts, delay, ex);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    delay = delay * 2;
                }
            }
            log.error("[VMAdapter] exhausted retries writing {} points", drained.size());
            requeueAtFront(drained);
        } catch (Exception ex) {
            log.error("[VMAdapter] unexpected flush error", ex);
            requeueAtFront(drained);
        }
    }

    int pendingPointCount() {
        return queue.size();
    }

    private void requeueAtFront(List<TrafficMetric> points) {
        for (int index = points.size() - 1; index >= 0; index--) {
            queue.offerFirst(points.get(index));
        }
    }

    private boolean isAbsoluteUrl(String url) {
        try {
            URI u = new URI(url);
            return u.isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    private String toLineProtocol(List<TrafficMetric> points) {
        StringBuilder sb = new StringBuilder();
        for (TrafficMetric p : points) {
            sb.append("traffic_metric");
            if (p.roadId() != null) sb.append(",roadId=").append(escapeTag(p.roadId()));
            if (p.direction() != null) sb.append(",direction=").append(escapeTag(p.direction()));
            if (p.vehicleType() != null) sb.append(",vehicleType=").append(escapeTag(p.vehicleType()));
            if (p.location() != null) sb.append(",location=").append(escapeTag(p.location()));
            sb.append(' ');
            boolean first = true;
            if (p.trafficFlow() != null) {
                sb.append("trafficFlow=").append(p.trafficFlow()).append('i');
                first = false;
            }
            if (p.averageSpeed() != null) {
                if (!first) sb.append(',');
                sb.append("averageSpeed=").append(p.averageSpeed());
                first = false;
            }
            if (p.co2Emission() != null) {
                if (!first) sb.append(',');
                sb.append("co2Emission=").append(p.co2Emission());
            }
            Instant ts = p.timestamp();
            long nanos = ts.getEpochSecond() * 1_000_000_000L + ts.getNano();
            sb.append(' ').append(nanos).append('\n');
        }
        return sb.toString();
    }

    private String escapeTag(String v) {
        return v.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
    }
}
