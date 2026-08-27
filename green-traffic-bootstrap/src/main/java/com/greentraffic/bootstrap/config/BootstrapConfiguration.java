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
        return null;
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
        return new com.greentraffic.core.application.SimulationOrchestrationService(publisher);
    }
}
