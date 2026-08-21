package com.greentraffic.api.controller;

import com.greentraffic.core.application.MetricService;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricService metricService;

    public MetricsController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/write")
    public ResponseEntity<String> write(@RequestBody TrafficMetric metric) {
        metricService.write(metric);
        return ResponseEntity.ok("accepted");
    }
}
