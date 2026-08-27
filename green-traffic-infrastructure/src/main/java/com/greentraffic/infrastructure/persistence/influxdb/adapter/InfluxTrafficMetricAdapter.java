package com.greentraffic.infrastructure.persistence.influxdb.adapter;

import com.greentraffic.core.domain.traffic.TrafficMetric;
import com.greentraffic.core.port.output.TrafficMetricStore;
import com.greentraffic.core.port.output.metrics.TrafficMetricQuery;
import com.greentraffic.infrastructure.persistence.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "influx")
public class InfluxTrafficMetricAdapter implements TrafficMetricStore {

    private static final String MEASUREMENT = "traffic_metric";

    private final InfluxDBClient client;
    private final InfluxDbProperties properties;

    public InfluxTrafficMetricAdapter(InfluxDbClientProvider clientProvider, InfluxDbProperties properties) {
        this.client = clientProvider.getClient();
        this.properties = properties;
    }

    @Override
    public void write(List<TrafficMetric> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        client.getWriteApiBlocking().writePoints(
                properties.getBucket(),
                properties.getOrg(),
                points.stream().map(this::toPoint).toList()
        );
    }

    @Override
    public List<TrafficMetric> query(TrafficMetricQuery query) {

        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "%s")
                  |> pivot(
                      rowKey: ["_time"],
                      columnKey: ["_field"],
                      valueColumn: "_value"
                  )
                """.formatted(
                escapeFlux(properties.getBucket()),
                query.from(),
                query.to(),
                MEASUREMENT
        );

        List<TrafficMetric> result = new ArrayList<>();

        for (FluxTable table :
                client.getQueryApi().query(flux, properties.getOrg())) {

            for (FluxRecord record : table.getRecords()) {

                result.add(new TrafficMetric(
                        stringValue(record, "roadId"),
                        stringValue(record, "direction"),
                        stringValue(record, "vehicleType"),
                        integerValue(record, "trafficFlow"),
                        doubleValue(record, "averageSpeed"),
                        doubleValue(record, "co2Emission"),
                        stringValue(record, "location"),
                        record.getTime()
                ));
            }
        }

        return result;
    }

    private Point toPoint(TrafficMetric point) {
        Point influxPoint = Point.measurement(MEASUREMENT)
                .time(point.timestamp() == null ? Instant.now() : point.timestamp(), WritePrecision.NS);
        influxPoint.addTag("source", "simulator");
        addTag(influxPoint, "roadId", point.roadId());
        addTag(influxPoint, "direction", point.direction());
        addTag(influxPoint, "vehicleType", point.vehicleType());
        addTag(influxPoint, "location", point.location());
        if (point.trafficFlow() != null) {
            influxPoint.addField("trafficFlow", point.trafficFlow());
        }
        if (point.averageSpeed() != null) {
            influxPoint.addField("averageSpeed", point.averageSpeed());
        }
        if (point.co2Emission() != null) {
            influxPoint.addField("co2Emission", point.co2Emission());
        }
        return influxPoint;
    }

    private void addTag(Point point, String key, String value) {
        if (value != null) {
            point.addTag(key, value);
        }
    }

    private String stringValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value == null ? null : value.toString();
    }

    private Integer integerValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double doubleValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private String escapeFlux(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String tagFilters(Map<String, String> tags) {
        StringBuilder filters = new StringBuilder();
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            filters.append("  |> filter(fn: (r) => r[\"")
                    .append(escapeFlux(tag.getKey()))
                    .append("\"] == \"")
                    .append(escapeFlux(tag.getValue()))
                    .append("\")\n");
        }
        return filters.toString();
    }
}
