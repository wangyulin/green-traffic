package com.greentraffic.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrafficFlowDTO {

    /**
     * 传感器ID
     */
    private String sensorId;

    /**
     * 道路ID
     */
    private String roadId;

    /**
     * 车辆总数
     */
    private Integer vehicleCount;

    /**
     * 平均速度
     */
    private Double averageSpeed;

    /**
     * 小汽车数量
     */
    private Integer carCount;

    /**
     * 公交车数量
     */
    private Integer busCount;

    /**
     * 卡车数量
     */
    private Integer truckCount;

    /**
     * 新能源车辆数量
     */
    private Integer newEnergyCount;

    /**
     * 采集时间
     */
    private LocalDateTime collectTime;
}