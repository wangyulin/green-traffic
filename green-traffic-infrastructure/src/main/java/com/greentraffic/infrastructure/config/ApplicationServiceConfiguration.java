package com.greentraffic.infrastructure.config;

import com.greentraffic.core.application.MetricApplicationService;
import com.greentraffic.core.application.SimulationMetricApplicationService;
import com.greentraffic.core.application.TrafficMetricQueryApplicationService;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.SimulationMetricStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure-side configuration to instantiate core application services
 * and wire them with infrastructure ports. This moves Spring annotations
 * out of `core` into `infrastructure`.
 */
@Configuration
public class ApplicationServiceConfiguration {

    @Bean
    public WriteTrafficMetricUseCase writeTrafficMetricUseCase(TrafficMetricStore writePort) {
        return new MetricApplicationService(writePort);
    }

    @Bean
    public WriteSimulationTrafficMetricUseCase writeSimulationTrafficMetricUseCase(SimulationMetricStore writePort) {
        return new SimulationMetricApplicationService(writePort);
    }

    @Bean
    public com.greentraffic.core.port.input.SimulationOrchestrationUseCase simulationOrchestrationUseCase(com.greentraffic.core.port.output.messaging.MessagePublisher publisher) {
        return new com.greentraffic.core.application.SimulationOrchestrationService(publisher);
    }

    @Bean
    public QueryTrafficMetricUseCase queryTrafficMetricUseCase(TrafficMetricStore queryPort) {
        return new TrafficMetricQueryApplicationService(queryPort);
    }
}
