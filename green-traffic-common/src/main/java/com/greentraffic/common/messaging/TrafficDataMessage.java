package com.greentraffic.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交通数据消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficDataMessage implements Serializable {

    private String roadId;
    private String vehicleType;
    private Integer trafficFlow;
    private Double averageSpeed;
    private Double co2Emission;
    private LocalDateTime timestamp;
    private String location;
}
