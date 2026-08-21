package com.greentraffic.infrastructure.persistence.influxdb.config;

import com.greentraffic.infrastructure.persistence.influxdb.InfluxDBProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.concurrent.TimeUnit;

/**
 * InfluxDB 配置类
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "traffic.influxdb", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InfluxDBConfig {

    private final InfluxDBProperties properties;

    /**
     * 创建 InfluxDB 客户端
     */
    @Bean
    @Primary
    public InfluxDBClient influxDBClient() {
        log.info("初始化 InfluxDB 客户端: URL={}, Org={}, Bucket={}",
                properties.getUrl(),
                properties.getOrg(),
                properties.getBucket());

        // 方式1：简单创建（推荐）
        InfluxDBClient client = InfluxDBClientFactory.create(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getOrg(),
                properties.getBucket()
        );

        // 测试连接
        testConnection(client);

        return client;
    }

    /**
     * 创建带自定义配置的客户端（使用 OkHttpClient）
     */
    @Bean
    public InfluxDBClient influxDBClientWithCustomHttp() {
        log.info("创建带自定义 HTTP 配置的 InfluxDB 客户端");

        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> log.debug("InfluxDB HTTP: {}", message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        // 创建 OkHttp 客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

        // 使用 InfluxDBClientOptions，但不设置 okHttpClient
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(properties.getUrl())
                .authenticateToken(properties.getToken().toCharArray())
                .org(properties.getOrg())
                .bucket(properties.getBucket())
                .build();

        // 创建客户端
        InfluxDBClient client = InfluxDBClientFactory.create(options);

        // 测试连接
        testConnection(client);

        return client;
    }

    /**
     * 测试连接
     */
    private void testConnection(InfluxDBClient client) {
        try {
            boolean pingResult = client.ping();
            if (pingResult) {
                log.info("InfluxDB 连接成功");
            } else {
                log.warn("InfluxDB 连接失败，请检查配置");
            }
        } catch (Exception e) {
            log.error("InfluxDB 连接异常", e);
        }
    }
}