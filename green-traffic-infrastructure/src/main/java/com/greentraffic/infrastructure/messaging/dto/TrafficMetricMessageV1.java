package com.greentraffic.infrastructure.messaging.dto;

import java.time.Instant;

public class TrafficMetricMessageV1 {

    private String roadId;
    private String direction;
    private String vehicleType;
    private Integer trafficFlow;
    private Double averageSpeed;
    private Double co2Emission;
    private String location;
    private Instant timestamp;

    public TrafficMetricMessageV1() {
    }

    public TrafficMetricMessageV1(
            String roadId,
            String direction,
            String vehicleType,
            Integer trafficFlow,
            Double averageSpeed,
            Double co2Emission,
            String location,
            Instant timestamp) {
        this.roadId = roadId;
        this.direction = direction;
        this.vehicleType = vehicleType;
        this.trafficFlow = trafficFlow;
        this.averageSpeed = averageSpeed;
        this.co2Emission = co2Emission;
        this.location = location;
        this.timestamp = timestamp;
    }

    public String getRoadId() {
        return roadId;
    }

    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getTrafficFlow() {
        return trafficFlow;
    }

    public void setTrafficFlow(Integer trafficFlow) {
        this.trafficFlow = trafficFlow;
    }

    public Double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public Double getCo2Emission() {
        return co2Emission;
    }

    public void setCo2Emission(Double co2Emission) {
        this.co2Emission = co2Emission;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
