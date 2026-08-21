package com.greentraffic.infrastructure.influxdb;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.common.repository.AsyncTrafficDataRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * InfluxDB 异步交通数据存储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InfluxDBAsyncTrafficDataRepository implements AsyncTrafficDataRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties properties;

    @Override
    public CompletableFuture<List<TrafficDataMessage>> asyncFindByRoadId(
            String roadId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return CompletableFuture.supplyAsync(() -> {
            List<TrafficDataMessage> results = new ArrayList<>();

            try {
                String fluxQuery = buildQueryString(roadId, startTime, endTime);
                QueryApi queryApi = influxDBClient.getQueryApi();

                // 使用同步查询，但在异步线程中执行
                var tables = queryApi.query(fluxQuery, properties.getOrg());

                for (var table : tables) {
                    for (FluxRecord record : table.getRecords()) {
                        TrafficDataMessage data = parseRecord(record);
                        if (data != null) {
                            results.add(data);
                        }
                    }
                }

                log.info("异步查询完成: RoadId={}, 结果数={}", roadId, results.size());

            } catch (Exception e) {
                log.error("异步查询失败", e);
            }

            return results;
        });
    }

    @Override
    public CompletableFuture<Double> asyncFindAverageCo2Emission(
            String roadId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String fluxQuery = buildAverageCo2Query(roadId, startTime, endTime);
                QueryApi queryApi = influxDBClient.getQueryApi();

                var tables = queryApi.query(fluxQuery, properties.getOrg());

                for (var table : tables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        }
                    }
                }

                return 0.0;

            } catch (Exception e) {
                log.error("异步查询平均碳排放失败", e);
                return 0.0;
            }
        });
    }

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

        query.append("|> limit(n: 1000)");

        return query.toString();
    }

    private String buildAverageCo2Query(String roadId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("from(bucket: \"%s\") ", properties.getBucket()));
        query.append(String.format("|> range(start: %s, stop: %s) ",
                formatTime(startTime), formatTime(endTime)));
        query.append("|> filter(fn: (r) => r._measurement == \"traffic_data\") ");
        query.append("|> filter(fn: (r) => r._field == \"co2_emission\") ");

        if (roadId != null && !roadId.isEmpty()) {
            query.append(String.format("|> filter(fn: (r) => r.road_id == \"%s\") ", roadId));
        }

        query.append("|> mean()");

        return query.toString();
    }

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

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "now()";
        }
        return dateTime.atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
                .toString();
    }
}