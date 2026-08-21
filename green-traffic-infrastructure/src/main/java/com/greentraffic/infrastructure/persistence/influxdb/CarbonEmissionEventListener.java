package com.greentraffic.infrastructure.persistence.influxdb;

import com.alibaba.fastjson2.JSON;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import com.greentraffic.core.application.MetricService;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import com.greentraffic.common.util.TimezoneUtils;
import java.util.HashMap;
import java.util.Map;

@Component
public class CarbonEmissionEventListener {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties properties;
    private final MetricService metricService;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CarbonEmissionEventListener(ObjectProvider<InfluxDbClientProvider> clientProviderOpt,
                                       InfluxDbProperties properties,
                                       ObjectProvider<MetricService> metricServiceOpt) {
        logger.info("CarbonEmissionEventListener 初始化开始 --->>> ");

        InfluxDbClientProvider provider = clientProviderOpt.getIfAvailable();
        this.influxDBClient = provider == null ? null : provider.getClient();
        this.properties = properties;
        MetricService ms = metricServiceOpt.getIfAvailable();
        this.metricService = ms;
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

            // 如果 Influx 可用则写入 Influx，否则通过 MetricService 写入（VictoriaMetrics）
            if (influxDBClient != null && properties != null && properties.isEnabled()) {
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
                writeApi.writePoint(p);
                logger.debug("写入 InfluxDB 成功: road={}, ts={}", event.roadId(), event.timestamp());
            } else if (metricService != null) {
                // 将事件走 MetricService，MetricService 会路由到 VictoriaMetricAdapter（vm 配置时）
                try {
                    metricService.write(event);
                    logger.debug("通过 MetricService 写入指标（VictoriaMetrics 路径）: road={}, ts={}", event.roadId(), event.timestamp());
                } catch (Exception ex) {
                    logger.error("通过 MetricService 写入指标失败", ex);
                }
            } else {
                logger.debug("既无 Influx 客户端也无 MetricService，无法写入指标，已忽略");
            }
        } catch (Exception e) {
            logger.error("仿真事件写入 InfluxDB 失败", e);
        }
    }
}
