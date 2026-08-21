package com.greentraffic.infrastructure;

import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.infrastructure.influxdb.InfluxDBProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * InfluxDB 异步查询服务
 * 提供异步查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBAsyncQueryService {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties properties;

    /**
     * 异步查询交通数据
     */
        public CompletableFuture<List<TrafficMetric>> asyncFindByRoadId(
            String roadId,
            Instant startTime,
            Instant endTime) {

        return CompletableFuture.supplyAsync(() -> {
            List<TrafficMetric> results = new ArrayList<>();

            try {
                String fluxQuery = buildQueryString(roadId, startTime, endTime);
                QueryApi queryApi = influxDBClient.getQueryApi();

                // 使用异步查询方法（带 BiConsumer）
                queryApi.query(
                        fluxQuery,
                        properties.getOrg(),
                        (cancellable, record) -> {
                            // 处理每条记录
                            TrafficMetric metric = parseRecord(record);
                            if (metric != null) {
                                synchronized (results) {
                                    results.add(metric);
                                }
                            }
                        },
                        throwable -> {
                            // 错误处理
                            log.error("异步查询失败", throwable);
                        },
                        () -> {
                            // 完成回调
                            log.info("异步查询完成，结果数: {}", results.size());
                        }
                );

            } catch (Exception e) {
                log.error("异步查询异常", e);
            }

            return results;
        });
    }

    /**
     * 构建查询语句
     */
    private String buildQueryString(String roadId,
                                    Instant startTime,
                                    Instant endTime) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("from(bucket: \"%s\") ", properties.getBucket()));
        query.append(String.format("|> range(start: %s, stop: %s) ",
                formatTime(startTime), formatTime(endTime)));
        query.append("|> filter(fn: (r) => r._measurement == \"traffic_data\") ");

        if (roadId != null && !roadId.isEmpty()) {
            query.append(String.format("|> filter(fn: (r) => r.road_id == \"%s\") ", roadId));
        }

        query.append("|> limit(n: 100)");

        return query.toString();
    }

    /**
     * 解析记录
     */
    private TrafficMetric parseRecord(FluxRecord record) {
        try {
            String roadId = record.getValueByKey("road_id") != null ? record.getValueByKey("road_id").toString() : null;
            String vehicleType = record.getValueByKey("vehicle_type") != null ? record.getValueByKey("vehicle_type").toString() : null;
            String location = record.getValueByKey("location") != null ? record.getValueByKey("location").toString() : null;

            String field = record.getField();
            Object value = record.getValue();

            Integer trafficFlow = null;
            Double averageSpeed = null;
            Double co2 = null;

            if (field != null && value != null) {
                switch (field) {
                    case "traffic_flow":
                        trafficFlow = ((Number) value).intValue();
                        break;
                    case "average_speed":
                        averageSpeed = ((Number) value).doubleValue();
                        break;
                    case "co2_emission":
                        co2 = ((Number) value).doubleValue();
                        break;
                }
            }

            Instant instant = record.getTime();

            return new TrafficMetric(
                    roadId,
                    null,
                    vehicleType,
                    trafficFlow,
                    averageSpeed,
                    co2,
                    location,
                    instant
            );

        } catch (Exception e) {
            log.error("解析记录失败", e);
            return null;
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(Instant instant) {
        if (instant == null) {
            return "now()";
        }
        return instant.toString();
    }
}