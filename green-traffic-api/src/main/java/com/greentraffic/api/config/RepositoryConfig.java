package com.greentraffic.api.config;

import com.greentraffic.core.port.output.TrafficDataRepository;
import com.greentraffic.infrastructure.persistence.influxdb.InfluxDBTrafficDataRepository;
import com.greentraffic.infrastructure.persistence.mysql.MySQLTrafficDataRepository;
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

    private final ObjectProvider<InfluxDBTrafficDataRepository> influxDBRepositoryProvider;
    private final ObjectProvider<MySQLTrafficDataRepository> mysqlRepositoryProvider;

    @Value("${metrics.sink:}")
    private String metricsSink;

    @Value("${storage.type:influxdb}")
    private String storageType;

    @Bean
    @Primary
    public TrafficDataRepository trafficDataRepository() {
        // If metrics are sent to VictoriaMetrics, we don't have a Time-series query-backed
        // TrafficDataRepository implementation available. Provide a lightweight stub
        // to satisfy beans that depend on TrafficDataRepository (queries will return empty).
        if (metricsSink != null && metricsSink.equalsIgnoreCase("vm")) {
            log.info("metrics.sink=vm，提供轻量的 TrafficDataRepository stub（查询为空，写操作受限）");
            return new TrafficDataRepository() {
                @Override
                public boolean save(com.greentraffic.model.entity.traffic.TrafficMetric data) {
                    // 写入操作应走 Metric pipeline (MetricWritePort)，不通过此接口
                    log.warn("TrafficDataRepository.save called in vm mode — no-op");
                    return false;
                }

                @Override
                public boolean saveBatch(java.util.List<com.greentraffic.model.entity.traffic.TrafficMetric> dataList) {
                    log.warn("TrafficDataRepository.saveBatch called in vm mode — no-op");
                    return false;
                }

                @Override
                public java.util.List<com.greentraffic.model.entity.traffic.TrafficMetric> findByRoadId(String roadId, java.time.Instant startTime, java.time.Instant endTime) {
                    log.warn("TrafficDataRepository.findByRoadId called in vm mode — returning empty list");
                    return java.util.Collections.emptyList();
                }

                @Override
                public Double findAverageCo2Emission(String roadId, java.time.Instant startTime, java.time.Instant endTime) {
                    log.warn("TrafficDataRepository.findAverageCo2Emission called in vm mode — returning 0.0");
                    return 0.0;
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public void cleanOldData(java.time.Instant beforeTime) {
                    log.warn("TrafficDataRepository.cleanOldData called in vm mode — no-op");
                }
            };
        }
        switch (storageType.toLowerCase()) {
            case "mysql":
                log.info("使用 MySQL 存储实现");
                MySQLTrafficDataRepository mysqlRepository = mysqlRepositoryProvider.getIfAvailable();
                if (mysqlRepository == null) {
                    log.warn("未找到 MySQL 存储实现的 Bean（未激活 mysql profile 或未定义），尝试回退到 InfluxDB");
                    InfluxDBTrafficDataRepository influxFallback = influxDBRepositoryProvider.getIfAvailable();
                    if (influxFallback == null) {
                        throw new IllegalStateException("没有可用的存储实现：MySQL 和 InfluxDB 均不可用");
                    }
                    return influxFallback;
                }
                return mysqlRepository;
            case "influxdb":
            default:
                log.info("使用 InfluxDB 存储实现");
                InfluxDBTrafficDataRepository influxRepo = influxDBRepositoryProvider.getIfAvailable();
                if (influxRepo == null) {
                    log.warn("InfluxDB 存储实现不可用，尝试回退到 MySQL");
                    MySQLTrafficDataRepository mysqlFallback = mysqlRepositoryProvider.getIfAvailable();
                    if (mysqlFallback == null) {
                        throw new IllegalStateException("没有可用的存储实现：InfluxDB 和 MySQL 均不可用");
                    }
                    return mysqlFallback;
                }
                return influxRepo;
        }
    }
}
