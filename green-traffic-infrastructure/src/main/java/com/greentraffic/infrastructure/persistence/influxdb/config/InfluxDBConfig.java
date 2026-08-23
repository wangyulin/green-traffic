package com.greentraffic.infrastructure.persistence.influxdb.config;

import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * InfluxDB 配置类
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "influx")
@RequiredArgsConstructor
public class InfluxDBConfig {

    private final InfluxDbClientProvider clientProvider;

    /**
     * 创建 InfluxDB 客户端
     */
    @Bean
    @Primary
    public InfluxDBClient influxDBClient() {
        log.info("使用统一的 traffic.influxdb 客户端配置");
        return clientProvider.getClient();
    }
}