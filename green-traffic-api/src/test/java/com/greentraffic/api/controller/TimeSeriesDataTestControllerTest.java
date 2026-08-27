package com.greentraffic.api.controller;

import com.greentraffic.core.port.input.QueryTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
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

        @Mock
        private com.greentraffic.api.mapper.TrafficMetricApiMapper mapper;

    @InjectMocks
    private TimeSeriesDataTestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void writesSampleMetricThroughInputPort() throws Exception {
        when(mapper.toCommand(org.mockito.ArgumentMatchers.any())).thenReturn(
                new WriteTrafficMetricCommand(
                        "ROAD-001",
                        "EAST",
                        "CAR",
                        120,
                        42.5,
                        12.3,
                        null,
                        Instant.now()
                )
        );
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
        com.greentraffic.core.application.query.model.TrafficMetricView metric = new com.greentraffic.core.application.query.model.TrafficMetricView(
                "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, null,
                Instant.parse("2026-08-22T10:00:00Z")
        );
        when(queryUseCase.query(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(metric));

        when(mapper.toResponseList(org.mockito.ArgumentMatchers.any())).thenReturn(
                List.of(new com.greentraffic.api.controller.dto.TrafficMetricResponse(
                        "ROAD-001", "EAST", "CAR", 120, 42.5, 12.3, null,
                        Instant.parse("2026-08-22T10:00:00Z")
                ))
        );

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