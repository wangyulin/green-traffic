package com.greentraffic.infrastructure;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.infrastructure.influxdb.InfluxDBProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public CompletableFuture<List<TrafficDataMessage>> asyncFindByRoadId(
            String roadId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return CompletableFuture.supplyAsync(() -> {
            List<TrafficDataMessage> results = new ArrayList<>();

            try {
                String fluxQuery = buildQueryString(roadId, startTime, endTime);
                QueryApi queryApi = influxDBClient.getQueryApi();

                // 使用异步查询方法（带 BiConsumer）
                queryApi.query(
                        fluxQuery,
                        properties.getOrg(),
                        (cancellable, record) -> {
                            // 处理每条记录
                            TrafficDataMessage data = parseRecord(record);
                            if (data != null) {
                                synchronized (results) {
                                    results.add(data);
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
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {
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
    private TrafficDataMessage parseRecord(FluxRecord record) {
        try {
            TrafficDataMessage data = new TrafficDataMessage();

            Object roadIdObj = record.getValueByKey("road_id");
            Object vehicleTypeObj = record.getValueByKey("vehicle_type");
            Object locationObj = record.getValueByKey("location");

            data.setRoadId(roadIdObj != null ? roadIdObj.toString() : null);
            data.setVehicleType(vehicleTypeObj != null ? vehicleTypeObj.toString() : null);
            data.setLocation(locationObj != null ? locationObj.toString() : null);

            String field = record.getField();
            Object value = record.getValue();

            if (field != null && value != null) {
                switch (field) {
                    case "traffic_flow":
                        data.setTrafficFlow(((Number) value).intValue());
                        break;
                    case "average_speed":
                        data.setAverageSpeed(((Number) value).doubleValue());
                        break;
                    case "co2_emission":
                        data.setCo2Emission(((Number) value).doubleValue());
                        break;
                }
            }

            if (record.getTime() != null) {
                data.setTimestamp(LocalDateTime.ofInstant(
                        record.getTime(),
                        ZoneId.systemDefault()
                ));
            }

            return data;

        } catch (Exception e) {
            log.error("解析记录失败", e);
            return null;
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "now()";
        }
        return dateTime.atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
                .toString();
    }
}