package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.greentraffic.infrastructure.config.MetricsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class VictoriaMetricAdapterTest {

        @Test
        void springSelectsProductionConstructor() {
                try (AnnotationConfigApplicationContext context =
                                         new AnnotationConfigApplicationContext()) {
                    context.getEnvironment().getPropertySources().addFirst(
                            new MapPropertySource(
                                    "victoriaMetricAdapterTest",
                                    Map.of("traffic.storage.type", "victoria-metrics")
                            )
                    );
                        context.registerBean(MetricsProperties.class);
                        context.registerBean(org.springframework.web.client.RestTemplate.class);
                        context.registerBean("vmMetricsScheduler", java.util.concurrent.ScheduledExecutorService.class,
                                () -> java.util.concurrent.Executors.newSingleThreadScheduledExecutor());
                        context.registerBean(VictoriaMetricAdapter.class);
                        context.refresh();

                        assertThat(context.getBean(VictoriaMetricAdapter.class)).isNotNull();
                }
        }

    @Test
    void requeuesDrainedBatchWhenAllWriteAttemptsFail() {
        MetricsProperties properties = new MetricsProperties();
        properties.setVmUrl("http://localhost:8428/write");
        properties.setBatchSize(10);
        properties.setMaxRetries(0);
        properties.setRetryInitialDelayMs(0);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo(properties.getVmUrl()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        VictoriaMetricAdapter adapter = new VictoriaMetricAdapter(properties, restTemplate);
        TrafficMetric point = new TrafficMetric(
                "ROAD-001", "EAST", "CAR", 10, 30.0, 2.0, null, Instant.now()
        );

        adapter.write(List.of(point));
        adapter.flush();

        assertThat(adapter.pendingPointCount()).isEqualTo(1);
        server.verify();
    }

    @Test
    void keepsPointsQueuedWhenWriteUrlIsInvalid() {
        MetricsProperties properties = new MetricsProperties();
        properties.setVmUrl("");
        VictoriaMetricAdapter adapter = new VictoriaMetricAdapter(
                properties,
                new RestTemplate()
        );

        adapter.write(List.of(new TrafficMetric(
                "ROAD-002", "WEST", "BUS", 5, 20.0, 1.0, null, Instant.now()
        )));
        adapter.flush();

        assertThat(adapter.pendingPointCount()).isEqualTo(1);
    }

        @Test
        void aggregatesMultipleSeriesIntoSingleTrafficMetric() throws Exception {
                VictoriaMetricAdapter adapter = new VictoriaMetricAdapter(new MetricsProperties(), new org.springframework.web.client.RestTemplate());
                ObjectMapper mapper = new ObjectMapper();
                String json = "{\"status\":\"success\",\"data\":{\"result\":["
                                + "{\"metric\":{\"__name__\":\"traffic_metric_trafficFlow\",\"roadId\":\"R1\",\"direction\":\"N\",\"vehicleType\":\"car\",\"location\":\"L1\"},\"values\":[[1620000000,\"10\"]]},"
                                + "{\"metric\":{\"__name__\":\"traffic_metric_averageSpeed\",\"roadId\":\"R1\",\"direction\":\"N\",\"vehicleType\":\"car\",\"location\":\"L1\"},\"values\":[[1620000000,\"42.5\"]]},"
                                + "{\"metric\":{\"__name__\":\"traffic_metric_co2Emission\",\"roadId\":\"R1\",\"direction\":\"N\",\"vehicleType\":\"car\",\"location\":\"L1\"},\"values\":[[1620000000,\"1.23\"]]}"
                                + "]}}";

                JsonNode node = mapper.readTree(json);
                List<TrafficMetric> pts = adapter.toMetricPoints(node);

                assertThat(pts).hasSize(1);
                TrafficMetric m = pts.get(0);
                assertThat(m.roadId()).isEqualTo("R1");
                assertThat(m.direction()).isEqualTo("N");
                assertThat(m.vehicleType()).isEqualTo("car");
                assertThat(m.trafficFlow()).isEqualTo(10);
                assertThat(m.averageSpeed()).isEqualTo(42.5);
                assertThat(m.co2Emission()).isEqualTo(1.23);
                assertThat(m.location()).isEqualTo("L1");
                assertThat(m.timestamp()).isEqualTo(Instant.ofEpochSecond(1620000000L));
        }
}