package com.greentraffic.api.controller;

import com.greentraffic.core.application.MetricService;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@ConditionalOnProperty(name = "metrics.sink", havingValue = "vm")
@RequestMapping("test/vm")
public class VmTestController {
    private final MetricService metricService;

    public VmTestController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/write")
    public String write() {
        TrafficMetric metric = new TrafficMetric(
                "ROAD-VM-001",
                "NORTH",
                "CAR",
                88,
                36.7,
                5.5,
                null,
                Instant.now()
        );

        metricService.write(metric);
        return "OK";
    }

    @GetMapping("/ping")
    public String ping() {
        return "vm-test-ok";
    }
}
