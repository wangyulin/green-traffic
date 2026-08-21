package com.greentraffic.core.repository;

import com.greentraffic.model.entity.TrafficData;

import java.time.Instant;
import java.util.List;

public interface TrafficRepository {

    void save(TrafficData data);

    List<TrafficData> query(Instant start, Instant stop);
}