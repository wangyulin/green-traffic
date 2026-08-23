package com.greentraffic.api.controller;

import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("test/time_series_data")
public class TimeSeriesDataTestController {

    private final WriteTrafficMetricUseCase writeUseCase;
    private final QueryTrafficMetricUseCase queryUseCase;

    public TimeSeriesDataTestController(WriteTrafficMetricUseCase writeUseCase,
                                        QueryTrafficMetricUseCase queryUseCase) {
        this.writeUseCase = writeUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping("/write")
    public String write() {

        WriteTrafficMetricCommand command = new WriteTrafficMetricCommand(
                "ROAD-001",
                "EAST",
            "CAR",
            120,
            42.5,
            12.3,
                null,
                Instant.now()
        );

        writeUseCase.write(command);

        return "OK";
    }

    @GetMapping("/read")
    public List<TrafficMetric> read() {

        Instant stop = Instant.now();
        Instant start = stop.minusSeconds(3600);

        return queryUseCase.query(start, stop);
    }
}
