package com.greentraffic.infrastructure.influxdb;

import com.alibaba.fastjson2.JSON;
import com.greentraffic.model.event.CarbonEmissionEvent;
import com.greentraffic.infrastructure.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;

@Component
public class CarbonEmissionEventListener {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties properties;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CarbonEmissionEventListener(InfluxDbClientProvider clientProvider, InfluxDbProperties properties) {
        this.influxDBClient = clientProvider.getClient();
        this.properties = properties;
    }

    @EventListener
    public void onCarbonEmission(CarbonEmissionEvent event) {
        try {
            logger.info("Spring Events message : {}", JSON.toJSONString(event));
            Point p = Point.measurement("traffic_flow")
                    .time(event.getTimestamp() == null ? Instant.now() : event.getTimestamp(), WritePrecision.S)
                    .addTag("road_id", event.getRoadId())
                    .addTag("direction", event.getDirection())
                    .addField("vehicle_count", event.getVehicleCount())
                    .addField("speed", event.getAverageSpeed())
                    .addField("co2_emission", event.getCo2Emission());

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            // 与 InfluxTrafficRepository 保持一致的写入方式
            writeApi.writePoint(p);
            logger.debug("写入 InfluxDB 成功: road={}, ts={}", event.getRoadId(), event.getTimestamp());
        } catch (Exception e) {
            logger.error("仿真事件写入 InfluxDB 失败", e);
        }
    }
}
