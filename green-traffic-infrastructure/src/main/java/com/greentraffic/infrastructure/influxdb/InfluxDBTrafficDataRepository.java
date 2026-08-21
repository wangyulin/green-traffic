package com.greentraffic.infrastructure.influxdb;

import com.greentraffic.common.repository.TrafficDataRepository;
import com.greentraffic.model.entity.traffic.TrafficMetric;
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
import java.time.OffsetDateTime;
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
    public boolean save(TrafficMetric data) {
        try {
            Point point = buildTrafficDataPoint(data);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoint(properties.getBucket(),
                    properties.getOrg(),
                    point);

                writeCount.incrementAndGet();
                log.debug("写入交通数据成功: RoadId={}, CO2={}",
                    data.roadId(), data.co2Emission());

            return true;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("写入交通数据失败: RoadId={}", data.roadId(), e);
            return false;
        }
    }

    @Override
    public boolean saveBatch(List<TrafficMetric> dataList) {
        try {
            List<Point> points = new ArrayList<>();
            for (TrafficMetric data : dataList) {
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
    public List<TrafficMetric> findByRoadId(String roadId,
                                                 Instant startTime,
                                                 Instant endTime) {
        List<TrafficMetric> results = new ArrayList<>();

        try {
            String fluxQuery = buildQueryString(roadId, startTime, endTime);

            QueryApi queryApi = influxDBClient.getQueryApi();

            // 方法1：使用 query(String, String) 返回 List<FluxTable>
            List<FluxTable> tables = queryApi.query(fluxQuery, properties.getOrg());

            // 解析结果
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    TrafficMetric metric = parseQueryRecord(record);
                    if (metric != null) {
                        results.add(metric);
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
                                         Instant startTime,
                                         Instant endTime) {
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
        public void cleanOldData(Instant beforeTime) {
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
    private Point buildTrafficDataPoint(TrafficMetric data) {
        Instant timestamp = data.timestamp() == null ? Instant.now() : data.timestamp();

        return Point.measurement("traffic_data")
                .time(timestamp, WritePrecision.MS)
                .addTag("road_id", data.roadId())
                .addTag("vehicle_type", data.vehicleType())
                .addTag("location", data.location())
                .addField("traffic_flow", data.trafficFlow())
                .addField("average_speed", data.averageSpeed())
                .addField("co2_emission", data.co2Emission());
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
                                        Instant startTime,
                                        Instant endTime) {
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
    private TrafficMetric parseQueryRecord(FluxRecord record) {
        try {
            // 解析标签
            String roadId = record.getValueByKey("road_id") != null ? record.getValueByKey("road_id").toString() : null;
            String vehicleType = record.getValueByKey("vehicle_type") != null ? record.getValueByKey("vehicle_type").toString() : null;
            String location = record.getValueByKey("location") != null ? record.getValueByKey("location").toString() : null;

            // 解析字段 - 由于 query 使用了 pivot，字段作为列出现在 record 中，使用 getValueByKey 读取
            Integer trafficFlow = null;
            Double averageSpeed = null;
            Double co2 = null;

            Object tf = record.getValueByKey("traffic_flow");
            if (tf instanceof Number) {
                trafficFlow = ((Number) tf).intValue();
            }

            Object avg = record.getValueByKey("average_speed");
            if (avg instanceof Number) {
                averageSpeed = ((Number) avg).doubleValue();
            }

            Object c = record.getValueByKey("co2_emission");
            if (c instanceof Number) {
                co2 = ((Number) c).doubleValue();
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
            log.error("解析查询记录失败", e);
            return null;
        }
    }

    /**
     * 转换时间
     */
    private String formatTimeForFlux(Instant instant) {
        if (instant == null) {
            return "now()";
        }
        return instant.toString();
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format("Write: %d, Errors: %d",
                writeCount.get(), errorCount.get());
    }
}
