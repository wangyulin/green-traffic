package com.greentraffic.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonEmissionEvent {
    private String roadId;
    private String direction;
    private Integer vehicleCount;
    private Double averageSpeed;
    private Double co2Emission;
    private Instant timestamp;
}
