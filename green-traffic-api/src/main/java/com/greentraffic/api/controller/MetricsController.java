package com.greentraffic.api.controller;

import com.greentraffic.api.controller.request.TrafficMetricWriteRequest;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final WriteTrafficMetricUseCase writeUseCase;

    public MetricsController(WriteTrafficMetricUseCase writeUseCase) {
        this.writeUseCase = writeUseCase;
    }

    @PostMapping("/write")
    public ResponseEntity<String> write(@RequestBody TrafficMetricWriteRequest request) {
        writeUseCase.write(request.toCommand());
        return ResponseEntity.ok("accepted");
    }
}
