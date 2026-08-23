package com.greentraffic.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "traffic.storage")
public class TrafficStorageProperties {

    private String type = "influx";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}