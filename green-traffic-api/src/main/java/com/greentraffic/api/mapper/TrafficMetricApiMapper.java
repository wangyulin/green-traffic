package com.greentraffic.api.mapper;

import com.greentraffic.api.controller.dto.TrafficMetricResponse;
import com.greentraffic.api.controller.dto.WriteTrafficMetricRequest;
import com.greentraffic.core.application.query.model.TrafficMetricView;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TrafficMetricApiMapper {

    public TrafficMetricResponse toResponse(TrafficMetricView domain) {
        if (domain == null) return null;
        return new TrafficMetricResponse(
                domain.roadId(),
                domain.direction(),
                domain.vehicleType(),
                domain.trafficFlow(),
                domain.averageSpeed(),
                domain.co2Emission(),
                domain.location(),
                domain.timestamp()
        );
    }

    public List<TrafficMetricResponse> toResponseList(List<TrafficMetricView> list) {
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WriteTrafficMetricCommand toCommand(WriteTrafficMetricRequest req) {
        if (req == null) return null;
        return new WriteTrafficMetricCommand(
                req.roadId(),
                req.direction(),
                req.vehicleType(),
                req.trafficFlow(),
                req.averageSpeed(),
                req.totalCo2Emission(),
                req.location(),
                req.timestamp()
        );
    }
}
