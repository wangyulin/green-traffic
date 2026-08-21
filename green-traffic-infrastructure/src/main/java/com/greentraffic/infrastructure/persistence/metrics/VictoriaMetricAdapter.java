package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.common.port.metrics.MetricPoint;
import com.greentraffic.common.port.metrics.MetricWritePort;
import com.greentraffic.infrastructure.config.MetricsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@ConditionalOnProperty(name = "metrics.sink", havingValue = "vm")
public class VictoriaMetricAdapter implements MetricWritePort {
    private static final Logger log = LoggerFactory.getLogger(VictoriaMetricAdapter.class);

    private final MetricsProperties props;
    private final RestTemplate rest;

    public VictoriaMetricAdapter(MetricsProperties props) {
        this.props = props;
        this.rest = new RestTemplate();
    }
    private final BlockingQueue<MetricPoint> queue = new LinkedBlockingQueue<>();
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
    public void write(List<MetricPoint> points) {
        if (points == null || points.isEmpty()) return;
        for (MetricPoint p : points) {
            queue.offer(p);
        }
        if (queue.size() >= Math.max(1, props.getBatchSize())) {
            // trigger immediate flush asynchronously
            scheduler.execute(this::flush);
        }
    }

    private synchronized void flush() {
        try {
            int batchSize = Math.max(1, props.getBatchSize());
            List<MetricPoint> drained = new ArrayList<>(batchSize);
            queue.drainTo(drained, batchSize);
            if (drained.isEmpty()) return;
            String payload = toLineProtocol(drained);
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
                    ResponseEntity<String> resp = rest.postForEntity(props.getVmUrl(), entity, String.class);
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
        } catch (Exception ex) {
            log.error("[VMAdapter] unexpected flush error", ex);
        }
    }

    private String toLineProtocol(List<MetricPoint> points) {
        StringBuilder sb = new StringBuilder();
        for (MetricPoint p : points) {
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
