package com.greentraffic.infrastructure.influxdb;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.common.repository.TrafficDataRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InfluxDB 交通数据存储实现
 * 使用同步查询方法，兼容不同版本
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InfluxDBTrafficDataRepository implements TrafficDataRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties properties;

    private final AtomicInteger writeCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @Override
    public boolean save(TrafficDataMessage data) {
        try {
            Point point = buildTrafficDataPoint(data);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoint(properties.getBucket(),
                    properties.getOrg(),
                    point);

            writeCount.incrementAndGet();
            log.debug("写入交通数据成功: RoadId={}, CO2={}",
                    data.getRoadId(), data.getCo2Emission());

            return true;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("写入交通数据失败: RoadId={}", data.getRoadId(), e);
            return false;
        }
    }

    @Override
    public boolean saveBatch(List<TrafficDataMessage> dataList) {
        try {
            List<Point> points = new ArrayList<>();
            for (TrafficDataMessage data : dataList) {
                points.add(buildTrafficDataPoint(data));
            }

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoints(properties.getBucket(),
                    properties.getOrg(),
                    points);

            writeCount.addAndGet(dataList.size());
            log.info("批量写入交通数据成功，共 {} 条", dataList.size());

            return true;

        } catch (Exception e) {
            errorCount.addAndGet(dataList.size());
            log.error("批量写入交通数据失败", e);
            return false;
        }
    }

    @Override
    public List<TrafficDataMessage> findByRoadId(String roadId,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime) {
        List<TrafficDataMessage> results = new ArrayList<>();

        try {
            String fluxQuery = buildQueryString(roadId, startTime, endTime);

            QueryApi queryApi = influxDBClient.getQueryApi();

            // 方法1：使用 query(String, String) 返回 List<FluxTable>
            List<FluxTable> tables = queryApi.query(fluxQuery, properties.getOrg());

            // 解析结果
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    TrafficDataMessage data = parseQueryRecord(record);
                    if (data != null) {
                        results.add(data);
                    }
                }
            }

            log.info("查询交通数据成功: RoadId={}, 结果数={}", roadId, results.size());

        } catch (Exception e) {
            log.error("查询交通数据失败: RoadId={}", roadId, e);
        }

        return results;
    }

    @Override
    public Double findAverageCo2Emission(String roadId,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime) {
        try {
            String fluxQuery = buildAverageCo2Query(roadId, startTime, endTime);

            QueryApi queryApi = influxDBClient.getQueryApi();

            // 使用同步查询方法
            List<FluxTable> tables = queryApi.query(fluxQuery, properties.getOrg());

            // 从结果中提取平均值
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Object value = record.getValue();
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                }
            }

            return 0.0;

        } catch (Exception e) {
            log.error("查询平均碳排放失败: RoadId={}", roadId, e);
            return 0.0;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return influxDBClient.ping();
        } catch (Exception e) {
            log.error("InfluxDB 连接检查失败", e);
            return false;
        }
    }

    @Override
    public void cleanOldData(LocalDateTime beforeTime) {
        try {
            String predicate = String.format(
                    "_measurement=\"traffic_data\" AND _time < %s",
                    formatTimeForFlux(beforeTime)
            );

            var deleteApi = influxDBClient.getDeleteApi();

            // 使用正确的 delete 方法
            deleteApi.delete(
                    OffsetDateTime.now().minusDays(properties.getRetentionDays()),
                    OffsetDateTime.now(),
                    predicate,
                    properties.getBucket(),
                    properties.getOrg()
            );

            log.info("清理过期数据成功，删除 {} 之前的数据", beforeTime);

        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }

    /**
     * 构建交通数据 Point
     */
    private Point buildTrafficDataPoint(TrafficDataMessage data) {
        Instant timestamp = convertToInstant(data.getTimestamp());

        return Point.measurement("traffic_data")
                .time(timestamp, WritePrecision.MS)
                .addTag("road_id", data.getRoadId())
                .addTag("vehicle_type", data.getVehicleType())
                .addTag("location", data.getLocation())
                .addField("traffic_flow", data.getTrafficFlow())
                .addField("average_speed", data.getAverageSpeed())
                .addField("co2_emission", data.getCo2Emission());
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
                formatTimeForFlux(startTime), formatTimeForFlux(endTime)));
        query.append("|> filter(fn: (r) => r._measurement == \"traffic_data\") ");

        if (roadId != null && !roadId.isEmpty()) {
            query.append(String.format("|> filter(fn: (r) => r.road_id == \"%s\") ", roadId));
        }

        query.append("|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") ");
        query.append("|> sort(columns: [\"_time\"], desc: true) ");
        query.append("|> limit(n: 1000)");

        return query.toString();
    }

    /**
     * 构建平均碳排放查询
     */
    private String buildAverageCo2Query(String roadId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("from(bucket: \"%s\") ", properties.getBucket()));
        query.append(String.format("|> range(start: %s, stop: %s) ",
                formatTimeForFlux(startTime), formatTimeForFlux(endTime)));
        query.append("|> filter(fn: (r) => r._measurement == \"traffic_data\") ");
        query.append("|> filter(fn: (r) => r._field == \"co2_emission\") ");

        if (roadId != null && !roadId.isEmpty()) {
            query.append(String.format("|> filter(fn: (r) => r.road_id == \"%s\") ", roadId));
        }

        query.append("|> mean()");

        return query.toString();
    }

    /**
     * 解析查询记录
     */
    private TrafficDataMessage parseQueryRecord(FluxRecord record) {
        try {
            TrafficDataMessage data = new TrafficDataMessage();

            // 解析标签
            Object roadIdObj = record.getValueByKey("road_id");
            Object vehicleTypeObj = record.getValueByKey("vehicle_type");
            Object locationObj = record.getValueByKey("location");

            data.setRoadId(roadIdObj != null ? roadIdObj.toString() : null);
            data.setVehicleType(vehicleTypeObj != null ? vehicleTypeObj.toString() : null);
            data.setLocation(locationObj != null ? locationObj.toString() : null);

            // 解析字段 - 使用 getField() 和 getValue()
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

            // 解析时间
            Instant instant = record.getTime();
            if (instant != null) {
                data.setTimestamp(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            }

            return data;

        } catch (Exception e) {
            log.error("解析查询记录失败", e);
            return null;
        }
    }

    /**
     * 转换时间
     */
    private Instant convertToInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Instant.now();
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 格式化时间为 Flux 查询格式
     */
    private String formatTimeForFlux(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "now()";
        }
        return dateTime.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("Write: %d, Errors: %d",
                writeCount.get(), errorCount.get());
    }
}
