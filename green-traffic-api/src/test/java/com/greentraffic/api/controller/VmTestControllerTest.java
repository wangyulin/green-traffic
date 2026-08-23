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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VmTestControllerTest {

    @Mock
    private WriteTrafficMetricUseCase writeUseCase;

    @InjectMocks
    private VmTestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void respondsToPing() throws Exception {
        mockMvc.perform(get("/test/vm/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("vm-test-ok"));
    }

    @Test
    void writesSampleMetricThroughInputPort() throws Exception {
        mockMvc.perform(post("/test/vm/write"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        ArgumentCaptor<WriteTrafficMetricCommand> commandCaptor = ArgumentCaptor.forClass(WriteTrafficMetricCommand.class);
        verify(writeUseCase).write(commandCaptor.capture());
        WriteTrafficMetricCommand command = commandCaptor.getValue();
        assertThat(command.roadId()).isEqualTo("ROAD-VM-001");
        assertThat(command.direction()).isEqualTo("NORTH");
    }
}