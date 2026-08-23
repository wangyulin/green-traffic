package com.greentraffic.api.controller;

import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "victoria-metrics")
@RequestMapping("test/vm")
public class VmTestController {
    private final WriteTrafficMetricUseCase writeUseCase;

    public VmTestController(WriteTrafficMetricUseCase writeUseCase) {
        this.writeUseCase = writeUseCase;
    }

    @PostMapping("/write")
    public String write() {
        WriteTrafficMetricCommand command = new WriteTrafficMetricCommand(
                "ROAD-VM-001",
                "NORTH",
                "CAR",
                88,
                36.7,
                5.5,
                null,
                Instant.now()
        );

        writeUseCase.write(command);
        return "OK";
    }

    @GetMapping("/ping")
    public String ping() {
        return "vm-test-ok";
    }
}
