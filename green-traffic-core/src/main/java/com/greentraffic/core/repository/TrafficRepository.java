package com.greentraffic.core.repository;

import com.greentraffic.model.entity.traffic.TrafficMetric;

import java.time.Instant;
import java.util.List;

public interface TrafficRepository {

    void save(TrafficMetric metric);

    List<TrafficMetric> query(Instant start, Instant stop);
}