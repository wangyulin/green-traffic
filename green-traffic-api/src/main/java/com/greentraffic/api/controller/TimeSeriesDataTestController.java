package com.greentraffic.api.controller;

import com.greentraffic.api.controller.dto.TrafficMetricResponse;
import com.greentraffic.api.controller.dto.WriteTrafficMetricRequest;
import com.greentraffic.api.mapper.TrafficMetricApiMapper;
import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;

@RestController
@Profile("debug")
@RequestMapping("test/time_series_data")
public class TimeSeriesDataTestController {

    private final WriteTrafficMetricUseCase writeUseCase;
    private final QueryTrafficMetricUseCase queryUseCase;
    private final TrafficMetricApiMapper mapper;

    public TimeSeriesDataTestController(WriteTrafficMetricUseCase writeUseCase,
                                        QueryTrafficMetricUseCase queryUseCase,
                                        TrafficMetricApiMapper mapper) {
        this.writeUseCase = writeUseCase;
        this.queryUseCase = queryUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/write")
    public String write() {

        WriteTrafficMetricRequest req = new WriteTrafficMetricRequest(
                "ROAD-001",
                "EAST",
                "CAR",
                120,
                42.5,
                12.3,
                null,
                Instant.now()
        );

        writeUseCase.write(mapper.toCommand(req));

        return "OK";
    }

    @GetMapping("/read")
    public List<TrafficMetricResponse> read() {

        Instant stop = Instant.now();
        Instant start = stop.minusSeconds(3600);

        return mapper.toResponseList(queryUseCase.query(start, stop));
    }
}
