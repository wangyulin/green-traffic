package com.greentraffic.infrastructure.persistence.influxdb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "traffic.influxdb")
public class InfluxDbProperties {
    private boolean enabled;

    private String url;

    private String token;

    private String org;

    private String bucket;

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration readTimeout = Duration.ofSeconds(30);

    private Duration writeTimeout = Duration.ofSeconds(10);
}
