package com.greentraffic.bootstrap.config;

import com.greentraffic.core.port.output.MetricQueryPort;
import com.greentraffic.core.port.output.MetricWritePort;
import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.SimulationMetricStore;
import com.greentraffic.core.port.output.messaging.MessagePublisher;
import com.greentraffic.core.port.output.messaging.MessageSubscriber;
import com.greentraffic.core.port.output.simulation.SimulationEnginePort;
import com.greentraffic.infrastructure.persistence.influxdb.adapter.InfluxTrafficMetricAdapter;
import com.greentraffic.infrastructure.persistence.metrics.VictoriaMetricAdapter;
import com.greentraffic.infrastructure.persistence.influxdb.adapter.InfluxSimulationMetricAdapter;
import com.greentraffic.infrastructure.persistence.metrics.VictoriaSimulationMetricAdapter;
import com.greentraffic.infrastructure.messaging.rocketmq.consumer.RocketMQMessageSubscriber;
import com.greentraffic.infrastructure.messaging.rocketmq.producer.RocketMQMessagePublisher;
import com.greentraffic.infrastructure.messaging.springevents.SpringEventsMessageSubscriber;
import com.greentraffic.infrastructure.messaging.springevents.SpringEventsMessagePublisher;
import com.greentraffic.infrastructure.simulation.DockerSimulationEngineAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(MetricWritePort.class)
    public MetricWritePort metricWritePort(
            ObjectProvider<InfluxTrafficMetricAdapter> influx,
            ObjectProvider<VictoriaMetricAdapter> victoria
    ) {
        InfluxTrafficMetricAdapter i = influx.getIfAvailable();
        if (i != null) return i;
        VictoriaMetricAdapter v = victoria.getIfAvailable();
        if (v != null) return v;
        throw new IllegalStateException("No MetricWritePort implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(TrafficMetricStore.class)
    public TrafficMetricStore trafficMetricStore(
            ObjectProvider<InfluxTrafficMetricAdapter> influx,
            ObjectProvider<VictoriaMetricAdapter> victoria
    ) {
        InfluxTrafficMetricAdapter i = influx.getIfAvailable();
        if (i != null) return i;
        VictoriaMetricAdapter v = victoria.getIfAvailable();
        if (v != null) return v;
        throw new IllegalStateException("No TrafficMetricStore implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(MetricQueryPort.class)
    public MetricQueryPort metricQueryPort(
            ObjectProvider<InfluxTrafficMetricAdapter> influx,
            ObjectProvider<VictoriaMetricAdapter> victoria
    ) {
        InfluxTrafficMetricAdapter i = influx.getIfAvailable();
        if (i != null) return i;
        VictoriaMetricAdapter v = victoria.getIfAvailable();
        if (v != null) return v;
        throw new IllegalStateException("No MetricQueryPort implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(SimulationMetricWritePort.class)
    public SimulationMetricWritePort simulationMetricWritePort(
            ObjectProvider<InfluxSimulationMetricAdapter> influxSim,
            ObjectProvider<VictoriaSimulationMetricAdapter> victoriaSim
    ) {
        InfluxSimulationMetricAdapter i = influxSim.getIfAvailable();
        if (i != null) return i;
        VictoriaSimulationMetricAdapter v = victoriaSim.getIfAvailable();
        if (v != null) return v;
        throw new IllegalStateException("No SimulationMetricWritePort implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(SimulationMetricStore.class)
    public SimulationMetricStore simulationMetricStore(
            ObjectProvider<InfluxSimulationMetricAdapter> influxSim,
            ObjectProvider<VictoriaSimulationMetricAdapter> victoriaSim
    ) {
        InfluxSimulationMetricAdapter i = influxSim.getIfAvailable();
        if (i != null) return i;
        VictoriaSimulationMetricAdapter v = victoriaSim.getIfAvailable();
        if (v != null) return v;
        throw new IllegalStateException("No SimulationMetricStore implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(MessageSubscriber.class)
    public MessageSubscriber messageSubscriber(
            ObjectProvider<RocketMQMessageSubscriber> rocket,
            ObjectProvider<SpringEventsMessageSubscriber> springEvents
    ) {
        RocketMQMessageSubscriber r = rocket.getIfAvailable();
        if (r != null) return r;
        SpringEventsMessageSubscriber s = springEvents.getIfAvailable();
        if (s != null) return s;
        throw new IllegalStateException("No MessageSubscriber implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(MessagePublisher.class)
    public MessagePublisher messagePublisher(
            ObjectProvider<RocketMQMessagePublisher> rocket,
            ObjectProvider<SpringEventsMessagePublisher> springEvents
    ) {
        RocketMQMessagePublisher r = rocket.getIfAvailable();
        if (r != null) return r;
        SpringEventsMessagePublisher s = springEvents.getIfAvailable();
        if (s != null) return s;
        throw new IllegalStateException("No MessagePublisher implementation available");
    }

    @Bean
    @ConditionalOnMissingBean(SimulationEnginePort.class)
    public SimulationEnginePort simulationEnginePort(ObjectProvider<DockerSimulationEngineAdapter> docker) {
        DockerSimulationEngineAdapter d = docker.getIfAvailable();
        if (d != null) return d;
        return null;
    }
}
