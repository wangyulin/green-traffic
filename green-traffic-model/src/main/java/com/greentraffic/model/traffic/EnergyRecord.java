package com.greentraffic.model.traffic;


import java.math.BigDecimal;
import java.time.Instant;

/**
 * 车辆能耗
 */
public class EnergyRecord {

    private String vehicleId;

    private BigDecimal energy;

    private Instant time;
}
