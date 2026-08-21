package com.greentraffic.infrastructure.influxdb;

import com.alibaba.fastjson2.JSON;
import com.greentraffic.model.entity.traffic.TrafficMetric;
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
import com.greentraffic.common.util.TimezoneUtils;
import java.util.HashMap;
import java.util.Map;

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
    public void onCarbonEmission(TrafficMetric event) {
        try {
            // 为了在日志中显示本地时区时间（例如 +08:00），先将 Instant 转为带时区偏移的字符串
            Map<String, Object> payload = new HashMap<>();
            payload.put("roadId", event.roadId());
            payload.put("direction", event.direction());
            payload.put("averageSpeed", event.averageSpeed());
            payload.put("co2Emission", event.co2Emission());
            payload.put("trafficFlow", event.trafficFlow());
            payload.put("vehicleType", event.vehicleType());
            String tsStr = event.timestamp() == null ? null : TimezoneUtils.formatForFlux(event.timestamp());
            payload.put("timestamp", tsStr);

            logger.info("Spring Events message : {}", JSON.toJSONString(payload));
                Instant ts = event.timestamp() == null ? Instant.now() : event.timestamp();
                ts = TimezoneUtils.normalizeInstant(ts);

                Point p = Point.measurement("traffic_flow")
                    .time(ts, WritePrecision.S)
                    .addTag("road_id", event.roadId())
                    .addTag("direction", event.direction())
                    .addField("vehicle_count", event.trafficFlow())
                    .addField("speed", event.averageSpeed())
                    .addField("co2_emission", event.co2Emission());

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            // 与 InfluxTrafficRepository 保持一致的写入方式
            writeApi.writePoint(p);
            logger.debug("写入 InfluxDB 成功: road={}, ts={}", event.roadId(), event.timestamp());
        } catch (Exception e) {
            logger.error("仿真事件写入 InfluxDB 失败", e);
        }
    }
}
