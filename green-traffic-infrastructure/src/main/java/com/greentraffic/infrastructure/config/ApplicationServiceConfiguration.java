package com.greentraffic.infrastructure.config;

import com.greentraffic.core.application.MetricApplicationService;
import com.greentraffic.core.application.SimulationMetricApplicationService;
import com.greentraffic.core.application.TrafficMetricQueryApplicationService;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.output.MetricWritePort;
import com.greentraffic.core.port.output.SimulationMetricWritePort;
import com.greentraffic.core.port.output.MetricQueryPort;
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
    public WriteTrafficMetricUseCase writeTrafficMetricUseCase(MetricWritePort writePort) {
        return new MetricApplicationService(writePort);
    }

    @Bean
    public WriteSimulationTrafficMetricUseCase writeSimulationTrafficMetricUseCase(SimulationMetricWritePort writePort) {
        return new SimulationMetricApplicationService(writePort);
    }

    @Bean
    public QueryTrafficMetricUseCase queryTrafficMetricUseCase(MetricQueryPort queryPort) {
        return new TrafficMetricQueryApplicationService(queryPort);
    }
}
