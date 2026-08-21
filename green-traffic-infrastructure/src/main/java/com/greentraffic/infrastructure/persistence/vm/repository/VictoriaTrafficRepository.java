package com.greentraffic.infrastructure.persistence.vm.repository;

import com.greentraffic.common.port.metrics.MetricPoint;
import com.greentraffic.common.port.metrics.MetricWritePort;
import com.greentraffic.core.repository.TrafficRepository;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "metrics.sink", havingValue = "vm")
public class VictoriaTrafficRepository implements TrafficRepository {

    private final MetricWritePort writePort;

    public VictoriaTrafficRepository(MetricWritePort writePort) {
        this.writePort = writePort;
    }

    @Override
    public void save(TrafficMetric metric) {
        MetricPoint p = toMetricPoint(metric);
        writePort.write(List.of(p));
    }

    @Override
    public List<TrafficMetric> query(Instant start, Instant stop) {
        // VictoriaMetrics querying is not implemented here. Return empty list for now.
        return new ArrayList<>();
    }

    private MetricPoint toMetricPoint(TrafficMetric tm) {
        Instant ts = tm.timestamp() == null ? Instant.now() : tm.timestamp();
        return new MetricPoint(
                tm.roadId(),
                tm.direction(),
                tm.vehicleType(),
                tm.trafficFlow(),
                tm.averageSpeed(),
                tm.co2Emission(),
                tm.location(),
                ts
        );
    }
}
