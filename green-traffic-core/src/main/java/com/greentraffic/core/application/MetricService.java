package com.greentraffic.core.application;

import com.greentraffic.common.port.metrics.MetricPoint;
import com.greentraffic.common.port.metrics.MetricWritePort;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetricService {
    private final MetricWritePort writePort;

    public MetricService(MetricWritePort writePort) {
        this.writePort = writePort;
    }

    public void write(TrafficMetric tm) {
        MetricPoint p = toMetricPoint(tm);
        writePort.write(List.of(p));
    }

    public void writeBatch(List<TrafficMetric> tms) {
        List<MetricPoint> points = tms.stream().map(this::toMetricPoint).toList();
        writePort.write(points);
    }

    private MetricPoint toMetricPoint(TrafficMetric tm) {
        return new MetricPoint(
                tm.roadId(),
                tm.direction(),
                tm.vehicleType(),
                tm.trafficFlow(),
                tm.averageSpeed(),
                tm.co2Emission(),
                tm.location(),
                tm.timestamp()
        );
    }
}
