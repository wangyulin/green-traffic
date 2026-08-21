package com.greentraffic.infrastructure.influxdb;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * InfluxDB 批量写入器
 * 提供批量写入和定时刷新功能
 *
 * 模块：green-traffic-infrastructure
 * 包：com.greentraffic.infrastructure.influxdb
 */
@Slf4j
public class InfluxDBBatchWriter {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBProperties properties;
    private final List<TrafficDataMessage> buffer = new ArrayList<>();
    private final Object lock = new Object();
    private final ScheduledExecutorService scheduler;

    public InfluxDBBatchWriter(InfluxDBClient influxDBClient,
                               InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.properties = properties;
        this.scheduler = Executors.newScheduledThreadPool(1);

        // 启动定时刷新任务
        scheduler.scheduleAtFixedRate(
                this::flush,
                properties.getFlushInterval(),
                properties.getFlushInterval(),
                TimeUnit.MILLISECONDS
        );

        log.info("InfluxDB 批量写入器已启动，批量大小: {}, 刷新间隔: {}ms",
                properties.getBatchSize(), properties.getFlushInterval());
    }

    /**
     * 添加数据到缓冲区
     */
    public void addToBatch(TrafficDataMessage data) {
        synchronized (lock) {
            buffer.add(data);

            if (buffer.size() >= properties.getBatchSize()) {
                flush();
            }
        }
    }

    /**
     * 批量添加数据
     */
    public void addAllToBatch(List<TrafficDataMessage> dataList) {
        synchronized (lock) {
            buffer.addAll(dataList);

            if (buffer.size() >= properties.getBatchSize()) {
                flush();
            }
        }
    }

    /**
     * 刷新缓冲区
     */
    public void flush() {
        synchronized (lock) {
            if (buffer.isEmpty()) {
                return;
            }

            List<TrafficDataMessage> batch = new ArrayList<>(buffer);
            buffer.clear();

            try {
                List<Point> points = new ArrayList<>();
                for (TrafficDataMessage data : batch) {
                    points.add(buildPoint(data));
                }

                WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
                writeApi.writePoints(properties.getBucket(),
                        properties.getOrg(),
                        points);

                log.info("批量写入成功，共 {} 条数据", batch.size());

            } catch (Exception e) {
                log.error("批量写入失败，重新加入缓冲区", e);
                // 失败的数据重新加入缓冲区
                buffer.addAll(0, batch);
            }
        }
    }

    /**
     * 构建数据点
     */
    private Point buildPoint(TrafficDataMessage data) {
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
     * 转换时间
     */
    private Instant convertToInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Instant.now();
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 获取缓冲区大小
     */
    public int getBufferSize() {
        synchronized (lock) {
            return buffer.size();
        }
    }

    /**
     * 关闭写入器
     */
    public void shutdown() {
        log.info("关闭 InfluxDB 批量写入器，执行最后一次刷新");
        flush();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}