package com.greentraffic.infrastructure.persistence.metrics;

import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VictoriaSimulationMetricAdapterTest {

    @Test
    void registersSimulationWritePortForVictoriaMetricsStorage() {
	try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
	    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
		    "victoriaSimulationMetricAdapterTest",
		    Map.of("traffic.storage.type", "victoria-metrics")
	    ));
	    context.registerBean(MetricsProperties.class);
	    context.registerBean(VictoriaSimulationMetricAdapter.class);
	    context.refresh();

			assertThat(context.getBean(SimulationMetricStore.class))
			.isInstanceOf(VictoriaSimulationMetricAdapter.class);
	}
    }

    @Test
    void writesSimulationPointUsingInfluxLineProtocol() {
	MetricsProperties properties = properties();
	RestTemplate restTemplate = new RestTemplate();
	MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
	server.expect(once(), requestTo(properties.getVmUrl()))
		.andExpect(method(HttpMethod.POST))
		.andExpect(content().string(
			"sumo_traffic_metric,source=sumo,simulationId=sim-1,roadId=ROAD-001,direction=EAST,vehicleType=CAR "
				+ "vehicleCount=10i,averageSpeed=30.5,totalCo2Emission=2.5,averageTravelTime=12.0,"
				+ "averageWaitingTime=1.0,averageTimeLoss=0.5,totalRouteLength=1000.0 "
				+ "1787738400000000000\n"
		))
		.andRespond(withSuccess());
	VictoriaSimulationMetricAdapter adapter =
		new VictoriaSimulationMetricAdapter(properties, restTemplate);

	adapter.write(List.of(point()));
	adapter.flush();

	assertThat(adapter.pendingPointCount()).isZero();
	server.verify();
    }

    @Test
    void requeuesSimulationPointsWhenWriteFails() {
	MetricsProperties properties = properties();
	properties.setMaxRetries(0);
	properties.setRetryInitialDelayMs(0);
	RestTemplate restTemplate = new RestTemplate();
	MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
	server.expect(once(), requestTo(properties.getVmUrl()))
		.andRespond(withServerError());
	VictoriaSimulationMetricAdapter adapter =
		new VictoriaSimulationMetricAdapter(properties, restTemplate);

	adapter.write(List.of(point()));
	adapter.flush();

	assertThat(adapter.pendingPointCount()).isEqualTo(1);
	server.verify();
    }

    private MetricsProperties properties() {
	MetricsProperties properties = new MetricsProperties();
	properties.setVmUrl("http://localhost:8428/write");
	properties.setBatchSize(10);
	return properties;
    }

	private SimulationTrafficMetric point() {
	return new SimulationTrafficMetric(
		"sim-1",
		"ROAD-001",
		"EAST",
		"CAR",
		10,
		30.5,
		2.5,
		12.0,
		1.0,
		0.5,
		1000.0,
		Instant.parse("2026-08-26T10:00:00Z")
	);
	}
}
