package com.greentraffic.infrastructure.persistence.influxdb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * InfluxDB 配置属性
 * 从 application.yml 中读取配置
 *
 * 模块：green-traffic-infrastructure
 * 包：com.greentraffic.infrastructure.influxdb
 */
@Data
@Component
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBProperties {

    /**
     * InfluxDB URL
     */
    private String url = "http://localhost:8086";

    /**
     * 认证 Token
     */
    private String token = "";

    /**
     * 组织名称
     */
    private String org = "greentraffic";

    /**
     * Bucket 名称
     */
    private String bucket = "traffic-data";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 10000;

    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 10000;

    /**
     * 是否启用 GZIP 压缩
     */
    private boolean gzipEnabled = true;

    /**
     * 批量写入大小
     */
    private int batchSize = 100;

    /**
     * 批量写入间隔（毫秒）
     */
    private long flushInterval = 5000;

    /**
     * 是否启用自动清理
     */
    private boolean autoCleanupEnabled = false;

    /**
     * 数据保留天数
     */
    private int retentionDays = 30;
}
