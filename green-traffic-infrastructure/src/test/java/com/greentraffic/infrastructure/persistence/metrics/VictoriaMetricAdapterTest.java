package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.port.output.metrics.MetricPoint;
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
        MetricPoint point = new MetricPoint(
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

        adapter.write(List.of(new MetricPoint(
                "ROAD-002", "WEST", "BUS", 5, 20.0, 1.0, null, Instant.now()
        )));
        adapter.flush();

        assertThat(adapter.pendingPointCount()).isEqualTo(1);
    }
}