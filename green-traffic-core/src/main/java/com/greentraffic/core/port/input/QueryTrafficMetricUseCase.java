package com.greentraffic.core.port.input;

import com.greentraffic.model.entity.traffic.TrafficMetric;

import java.time.Instant;
import java.util.List;

public interface QueryTrafficMetricUseCase {

    List<TrafficMetric> query(
            Instant start,
            Instant end
    );
}
