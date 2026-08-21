package com.greentraffic.api.controller;

import com.greentraffic.core.repository.TrafficRepository;
import com.greentraffic.model.entity.TrafficData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("test/influx")
public class InfluxTestController {
    private final TrafficRepository repository;

    public InfluxTestController(TrafficRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/write")
    public String write() {

        TrafficData data = new TrafficData(
                "ROAD-001",
                "EAST",
                120,
                42.5,
                Instant.now()
        );

        repository.save(data);

        return "OK";
    }

    @GetMapping("/read")
    public List<TrafficData> read() {

        Instant stop = Instant.now();
        Instant start = stop.minusSeconds(3600);

        return repository.query(start, stop);
    }
}
