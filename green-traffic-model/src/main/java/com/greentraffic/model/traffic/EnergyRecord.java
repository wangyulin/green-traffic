package com.greentraffic.model.traffic;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆能耗
 */
public class EnergyRecord {

    private String vehicleId;

    private BigDecimal energy;

    private LocalDateTime time;
}
