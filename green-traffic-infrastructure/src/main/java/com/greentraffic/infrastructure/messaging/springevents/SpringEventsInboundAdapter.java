package com.greentraffic.infrastructure.messaging.springevents;

import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteSimulationTrafficMetricUseCase;
import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;
import com.greentraffic.infrastructure.messaging.TrafficMetricMessageConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter for Spring Events path: registers the message handler
 * with the SpringEventsMessageSubscriber so published events are dispatched.
 */
@Component
@ConditionalOnProperty(name = "messaging.type", havingValue = "events", matchIfMissing = true)
public class SpringEventsInboundAdapter {

    private final TrafficMetricMessageConsumer handler;

    public SpringEventsInboundAdapter(
            WriteTrafficMetricUseCase writeUseCase,
            WriteSimulationTrafficMetricUseCase writeSimulationUseCase,
            SpringEventsMessageSubscriber subscriber) {
        this.handler = new TrafficMetricMessageConsumer(writeUseCase, writeSimulationUseCase);
        if (subscriber != null) {
            subscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA_BATCH, this.handler::consume);
            subscriber.subscribe(TrafficMessageTypes.TRAFFIC_DATA, this.handler::consume);
            subscriber.subscribe(TrafficMessageTypes.CO2_EMISSION, this.handler::consume);
        }
    }
}
