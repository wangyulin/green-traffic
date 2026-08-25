package com.greentraffic.api.controller;

import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import com.greentraffic.core.domain.traffic.TrafficMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimeSeriesDataTestControllerTest {

    @Mock
    private WriteTrafficMetricUseCase writeUseCase;

    @Mock
    private QueryTrafficMetricUseCase queryUseCase;

    @InjectMocks
    private TimeSeriesDataTestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void writesSampleMetricThroughInputPort() throws Exception {
        mockMvc.perform(post("/test/time_series_data/write"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        ArgumentCaptor<WriteTrafficMetricCommand> commandCaptor = ArgumentCaptor.forClass(WriteTrafficMetricCommand.class);
        verify(writeUseCase).write(commandCaptor.capture());
        WriteTrafficMetricCommand command = commandCaptor.getValue();
        assertThat(command.roadId()).isEqualTo("ROAD-001");
        assertThat(command.direction()).isEqualTo("EAST");
        assertThat(command.vehicleType()).isEqualTo("CAR");
    }

    @Test
    void readsMetricsThroughQueryInputPort() throws Exception {
        TrafficMetric metric = new TrafficMetric(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, null,
                Instant.parse("2026-08-22T10:00:00Z")
        );
        when(queryUseCase.query(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/test/time_series_data/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roadId").value("ROAD-001"))
                .andExpect(jsonPath("$[0].trafficFlow").value(120));

        verify(queryUseCase).query(
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        );
    }
}