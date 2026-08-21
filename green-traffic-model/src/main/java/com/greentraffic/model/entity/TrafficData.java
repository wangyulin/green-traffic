package com.greentraffic.model.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Accessors(fluent = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficData {

//    private Long id;
//    private String intersectionId;
//    private Integer vehicleCount;
//    private BigDecimal avgWaitTime;
//    private BigDecimal avgSpeed;
//    private BigDecimal truckRatio;
//    private BigDecimal carbonEmission;
//    private LocalDateTime createTime;
    private String roadId;
    private String direction;
    /**
     * vehicleCount（车辆数）
     * 合理范围：0 ~ 500 辆/统计周期
     *
     * 城市主干道：单车道每小时通行能力约 1500-2000 辆，如果统计周期是 5 分钟，单车道约 125-170 辆
     *
     * 多车道道路：3-4 车道的话，5 分钟统计周期内 300-500 辆是合理的
     *
     * 高速公路：单车道 5 分钟可达到 200-300 辆
     *
     * 异常值判断：超过 1000 可能表示数据异常或统计周期较长
     */
    private Integer vehicleCount;
    /**
     * speed（速度）
     * 合理范围：0 ~ 120 km/h
     *
     * 城市道路：0-60 km/h（拥堵时 0-20，畅通时 40-60）
     *
     * 快速路/高架：20-80 km/h
     *
     * 高速公路：60-120 km/h
     *
     * 异常值判断：
     *
     * 负数：数据错误
     *
     * 0：可能表示完全拥堵或停车
     *
     * 超过 150 km/h：可能超速或数据异常
     */
    private Double speed;
    private Instant time;
}