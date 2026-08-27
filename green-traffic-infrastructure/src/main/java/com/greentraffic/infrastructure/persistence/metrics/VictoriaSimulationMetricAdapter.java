package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
import com.greentraffic.infrastructure.config.MetricsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "victoria-metrics")
public class VictoriaSimulationMetricAdapter implements SimulationMetricWritePort, SimulationMetricStore {

	private static final Logger log = LoggerFactory.getLogger(VictoriaSimulationMetricAdapter.class);
	private static final String MEASUREMENT = "sumo_traffic_metric";

	private final MetricsProperties properties;
	private final RestTemplate restTemplate;
	private final BlockingDeque<SimulationTrafficMetric> queue = new LinkedBlockingDeque<>();
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
	public void start() {
		scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "vm-simulation-metrics-flusher");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(
				this::flush,
				properties.getFlushIntervalMs(),
				properties.getFlushIntervalMs(),
				TimeUnit.MILLISECONDS
		);
	}

	@PreDestroy
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
		flush();
	}

	@Override
	public void write(List<SimulationTrafficMetric> points) {
		if (points == null || points.isEmpty()) {
			return;
		}
		points.forEach(queue::offerLast);
		if (scheduler != null && queue.size() >= Math.max(1, properties.getBatchSize())) {
			scheduler.execute(this::flush);
		}
	}

	synchronized void flush() {
		List<SimulationTrafficMetric> drained = new ArrayList<>(Math.max(1, properties.getBatchSize()));
		queue.drainTo(drained, Math.max(1, properties.getBatchSize()));
		if (drained.isEmpty()) {
			return;
		}

		String url = properties.getVmUrl();
		if (url == null || url.isBlank() || !isAbsoluteUrl(url)) {
			log.error("[VMSimulationAdapter] invalid or missing metrics.vmUrl: {}", url);
			requeueAtFront(drained);
			return;
		}

		String payload = toLineProtocol(drained);
		int attempts = 0;
		long delay = properties.getRetryInitialDelayMs();
		while (attempts <= properties.getMaxRetries()) {
			try {
				restTemplate.postForEntity(
						url,
						new HttpEntity<>(payload, createHeaders()),
						String.class
				);
				return;
			} catch (Exception exception) {
				attempts++;
				log.warn(
						"[VMSimulationAdapter] write attempt {} failed, retrying after {} ms",
						attempts,
						delay,
						exception
				);
				try {
					Thread.sleep(delay);
				} catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					break;
				}
				delay *= 2;
			}
		}

		log.error("[VMSimulationAdapter] exhausted retries writing {} points", drained.size());
		requeueAtFront(drained);
	}

	int pendingPointCount() {
		return queue.size();
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		if ("bearer".equalsIgnoreCase(properties.getAuthType()) && properties.getToken() != null) {
			headers.setBearerAuth(properties.getToken());
		} else if ("basic".equalsIgnoreCase(properties.getAuthType()) && properties.getUsername() != null) {
			String credentials = properties.getUsername() + ":"
					+ (properties.getPassword() == null ? "" : properties.getPassword());
			headers.setBasicAuth(Base64.getEncoder().encodeToString(
					credentials.getBytes(StandardCharsets.UTF_8)
			));
		}
		return headers;
	}

	private void requeueAtFront(List<SimulationTrafficMetric> points) {
		for (int index = points.size() - 1; index >= 0; index--) {
			queue.offerFirst(points.get(index));
		}
	}

	private String toLineProtocol(List<SimulationTrafficMetric> points) {
		StringBuilder payload = new StringBuilder();
		for (SimulationTrafficMetric point : points) {
			payload.append(MEASUREMENT);
			appendTag(payload, "simulationId", point.simulationId());
			appendTag(payload, "roadId", point.roadId());
			appendTag(payload, "direction", point.direction());
			appendTag(payload, "vehicleType", point.vehicleType());
			payload.append(' ');

			boolean hasField = false;
			hasField = appendField(payload, "vehicleCount", point.vehicleCount(), hasField, true);
			hasField = appendField(payload, "averageSpeed", point.averageSpeed(), hasField, false);
			hasField = appendField(payload, "totalCo2Emission", point.totalCo2Emission(), hasField, false);
			hasField = appendField(payload, "averageTravelTime", point.averageTravelTime(), hasField, false);
			hasField = appendField(payload, "averageWaitingTime", point.averageWaitingTime(), hasField, false);
			hasField = appendField(payload, "averageTimeLoss", point.averageTimeLoss(), hasField, false);
			appendField(payload, "totalRouteLength", point.totalRouteLength(), hasField, false);

			Instant timestamp = point.timestamp() == null ? Instant.now() : point.timestamp();
			long nanos = timestamp.getEpochSecond() * 1_000_000_000L + timestamp.getNano();
			payload.append(' ').append(nanos).append('\n');
		}
		return payload.toString();
	}

	private void appendTag(StringBuilder payload, String name, String value) {
		if (value != null) {
			payload.append(',').append(name).append('=').append(escapeTag(value));
		}
	}

	private boolean appendField(
			StringBuilder payload,
			String name,
			Number value,
			boolean hasField,
			boolean integer
	) {
		if (value == null) {
			return hasField;
		}
		if (hasField) {
			payload.append(',');
		}
		payload.append(name).append('=').append(value);
		if (integer) {
			payload.append('i');
		}
		return true;
	}

	private String escapeTag(String value) {
		return value.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
	}

	private boolean isAbsoluteUrl(String url) {
		try {
			return new URI(url).isAbsolute();
		} catch (Exception exception) {
			return false;
		}
	}
}
