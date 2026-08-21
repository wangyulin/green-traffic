package com.greentraffic.api.config;

import com.greentraffic.common.repository.TrafficDataRepository;
import com.greentraffic.infrastructure.influxdb.InfluxDBTrafficDataRepository;
import com.greentraffic.infrastructure.mysql.MySQLTrafficDataRepository;
import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

/**
 * 存储实现选择配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RepositoryConfig {

    private final InfluxDBTrafficDataRepository influxDBRepository;
    private final ObjectProvider<MySQLTrafficDataRepository> mysqlRepositoryProvider;

    @Value("${storage.type:influxdb}")
    private String storageType;

    @Bean
    @Primary
    public TrafficDataRepository trafficDataRepository() {
        switch (storageType.toLowerCase()) {
            case "mysql":
                log.info("使用 MySQL 存储实现");
                MySQLTrafficDataRepository mysqlRepository = mysqlRepositoryProvider.getIfAvailable();
                if (mysqlRepository == null) {
                    log.warn("未找到 MySQL 存储实现的 Bean（未激活 mysql profile 或未定义），回退到 InfluxDB");
                    return influxDBRepository;
                }
                return mysqlRepository;
            case "influxdb":
            default:
                log.info("使用 InfluxDB 存储实现");
                return influxDBRepository;
        }
    }
}
