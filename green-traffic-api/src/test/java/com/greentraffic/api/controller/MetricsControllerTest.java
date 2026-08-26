package com.greentraffic.api.controller;

import com.greentraffic.core.port.input.WriteTrafficMetricUseCase;
import com.greentraffic.core.port.input.WriteTrafficMetricCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MetricsControllerTest {

    @Mock
    private WriteTrafficMetricUseCase writeUseCase;

    @InjectMocks
    private MetricsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void writesRequestMetricThroughInputPort() throws Exception {
        mockMvc.perform(post("/api/metrics/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roadId": "ROAD-100",
                                  "direction": "NORTH",
                                  "vehicleType": "CAR",
                                  "trafficFlow": 100,
                                  "averageSpeed": 42.5,
                                  "co2Emission": 10.2,
                                  "location": "Wangjing",
                                  "timestamp": "2026-08-22T10:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":200}"));

        ArgumentCaptor<WriteTrafficMetricCommand> commandCaptor = ArgumentCaptor.forClass(WriteTrafficMetricCommand.class);
        verify(writeUseCase).write(commandCaptor.capture());
        WriteTrafficMetricCommand command = commandCaptor.getValue();
        assertThat(command.roadId()).isEqualTo("ROAD-100");
        assertThat(command.trafficFlow()).isEqualTo(100);
        assertThat(command.timestamp()).hasToString("2026-08-22T10:00:00Z");
    }
}