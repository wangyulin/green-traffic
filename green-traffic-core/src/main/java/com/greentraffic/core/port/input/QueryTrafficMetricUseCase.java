package com.greentraffic.core.port.input;

import com.greentraffic.core.application.query.model.TrafficMetricView;

import java.time.Instant;
import java.util.List;

public interface QueryTrafficMetricUseCase {

    List<TrafficMetricView> query(
            Instant start,
            Instant end
    );
}
