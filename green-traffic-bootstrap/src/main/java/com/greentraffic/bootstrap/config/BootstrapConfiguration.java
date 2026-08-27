package com.greentraffic.bootstrap.config;

// legacy technical ports removed from bootstrap imports
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.simulation.SimulationEnginePort;
import com.greentraffic.infrastructure.persistence.influxdb.adapter.InfluxTrafficMetricAdapter;
import com.greentraffic.infrastructure.persistence.metrics.VictoriaMetricAdapter;
import com.greentraffic.infrastructure.persistence.influxdb.adapter.InfluxSimulationMetricAdapter;
import com.greentraffic.infrastructure.persistence.metrics.VictoriaSimulationMetricAdapter;
// RocketMQ/SpringEvents subscribers are replaced by explicit inbound adapters
import com.greentraffic.infrastructure.messaging.rocketmq.producer.RocketMQMessagePublisher;
// SpringEvents subscriber replaced by explicit inbound adapter
import com.greentraffic.infrastructure.messaging.springevents.SpringEventsMessagePublisher;
import com.greentraffic.infrastructure.simulation.DockerSimulationEngineAdapter;
import com.greentraffic.core.application.MetricApplicationService;
import com.greentraffic.core.application.SimulationMetricApplicationService;
import com.greentraffic.core.application.TrafficMetricQueryApplicationService;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(TrafficMetricStore.class)
    public TrafficMetricStore trafficMetricStore(
            @Value("${traffic.storage.type:influx}") String storageType,
            ObjectProvider<InfluxTrafficMetricAdapter> influx,
            ObjectProvider<VictoriaMetricAdapter> victoria
    ) {
        switch (storageType) {
            case "influx":
                InfluxTrafficMetricAdapter i = influx.getIfAvailable();
                if (i != null) return i;
                throw new IllegalStateException("traffic.storage.type=influx but no InfluxTrafficMetricAdapter available");
            case "victoria-metrics":
                VictoriaMetricAdapter v = victoria.getIfAvailable();
                if (v != null) return v;
                throw new IllegalStateException("traffic.storage.type=victoria-metrics but no VictoriaMetricAdapter available");
            default:
                throw new IllegalStateException("Unsupported traffic.storage.type='" + storageType + "'");
        }
    }

    @Bean
    @ConditionalOnMissingBean(SimulationMetricStore.class)
    public SimulationMetricStore simulationMetricStore(
            @Value("${traffic.storage.type:influx}") String storageType,
            ObjectProvider<InfluxSimulationMetricAdapter> influxSim,
            ObjectProvider<VictoriaSimulationMetricAdapter> victoriaSim
    ) {
        switch (storageType) {
            case "influx":
                InfluxSimulationMetricAdapter i = influxSim.getIfAvailable();
                if (i != null) return i;
                throw new IllegalStateException("traffic.storage.type=influx but no InfluxSimulationMetricAdapter available");
            case "victoria-metrics":
                VictoriaSimulationMetricAdapter v = victoriaSim.getIfAvailable();
                if (v != null) return v;
                throw new IllegalStateException("traffic.storage.type=victoria-metrics but no VictoriaSimulationMetricAdapter available");
            default:
                throw new IllegalStateException("Unsupported traffic.storage.type='" + storageType + "'");
        }
    }

    // MessageSubscriber assembly removed: inbound adapters now wire directly to handlers

    @Bean
    @ConditionalOnMissingBean(MessagePublisher.class)
    public MessagePublisher messagePublisher(
            @Value("${messaging.type:events}") String messagingType,
            ObjectProvider<RocketMQMessagePublisher> rocket,
            ObjectProvider<SpringEventsMessagePublisher> springEvents
    ) {
        switch (messagingType) {
            case "rocketmq":
                RocketMQMessagePublisher r = rocket.getIfAvailable();
                if (r != null) return r;
                throw new IllegalStateException("messaging.type=rocketmq but no RocketMQMessagePublisher available");
            case "events":
            default:
                SpringEventsMessagePublisher s = springEvents.getIfAvailable();
                if (s != null) return s;
                throw new IllegalStateException("messaging.type=events but no SpringEventsMessagePublisher available");
        }
    }

    @Bean
    @ConditionalOnMissingBean(SimulationEnginePort.class)
    public SimulationEnginePort simulationEnginePort(ObjectProvider<DockerSimulationEngineAdapter> docker) {
        DockerSimulationEngineAdapter d = docker.getIfAvailable();
        if (d != null) return d;
        throw new IllegalStateException("No SimulationEnginePort implementation available. Enable green-traffic.sumo.enabled or provide an adapter.");
    }

    @Bean
    @ConditionalOnMissingBean(WriteTrafficMetricUseCase.class)
    public WriteTrafficMetricUseCase writeTrafficMetricUseCase(TrafficMetricStore writePort) {
        return new MetricApplicationService(writePort);
    }

    @Bean
    @ConditionalOnMissingBean(WriteSimulationTrafficMetricUseCase.class)
    public WriteSimulationTrafficMetricUseCase writeSimulationTrafficMetricUseCase(SimulationMetricStore writePort) {
        return new SimulationMetricApplicationService(writePort);
    }

    @Bean
    @ConditionalOnMissingBean(QueryTrafficMetricUseCase.class)
    public QueryTrafficMetricUseCase queryTrafficMetricUseCase(TrafficMetricStore queryPort) {
        return new TrafficMetricQueryApplicationService(queryPort);
    }

    @Bean
    @ConditionalOnMissingBean(com.greentraffic.core.port.input.SimulationOrchestrationUseCase.class)
    public com.greentraffic.core.port.input.SimulationOrchestrationUseCase simulationOrchestrationUseCase(MessagePublisher publisher) {
        // Composition root creates domain/service dependencies and injects into core use-case
        // provide clock, id generator and random provider implementations for core's generator
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        com.greentraffic.core.port.util.IdGenerator idGen = () -> java.util.UUID.randomUUID().toString();
        com.greentraffic.core.port.util.RandomProvider randomProvider = new com.greentraffic.core.port.util.RandomProvider() {
            @Override
            public int nextInt(int bound) {
                return java.util.concurrent.ThreadLocalRandom.current().nextInt(bound);
            }

            @Override
            public double nextDouble(double origin, double bound) {
                return java.util.concurrent.ThreadLocalRandom.current().nextDouble(origin, bound);
            }
        };
        com.greentraffic.core.domain.simulation.SimulationTrafficGenerator generator = new com.greentraffic.core.domain.simulation.SimulationTrafficGenerator(clock, idGen, randomProvider);
        return new com.greentraffic.core.application.SimulationOrchestrationService(publisher, generator, idGen, clock);
    }

    @Bean
    @ConditionalOnMissingBean(com.greentraffic.core.port.util.IdGenerator.class)
    public com.greentraffic.core.port.util.IdGenerator idGenerator() {
        return () -> java.util.UUID.randomUUID().toString();
    }

    @Bean
    @ConditionalOnMissingBean(com.greentraffic.core.port.util.RandomProvider.class)
    public com.greentraffic.core.port.util.RandomProvider randomProvider() {
        return new com.greentraffic.core.port.util.RandomProvider() {
            @Override
            public int nextInt(int bound) { return java.util.concurrent.ThreadLocalRandom.current().nextInt(bound); }
            @Override
            public double nextDouble(double origin, double bound) { return java.util.concurrent.ThreadLocalRandom.current().nextDouble(origin, bound); }
        };
    }

    @Bean
    @ConditionalOnMissingBean(java.time.Clock.class)
    public java.time.Clock systemClock() {
        return java.time.Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnMissingBean(org.springframework.web.client.RestTemplate.class)
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }

    @Bean(name = "vmMetricsScheduler")
    @ConditionalOnMissingBean(name = "vmMetricsScheduler")
    public java.util.concurrent.ScheduledExecutorService vmMetricsScheduler() {
        return java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "vm-shared-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
}
