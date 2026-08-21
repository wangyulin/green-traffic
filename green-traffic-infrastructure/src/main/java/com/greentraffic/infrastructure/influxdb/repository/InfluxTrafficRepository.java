package com.greentraffic.infrastructure.influxdb.repository;

import com.greentraffic.core.repository.TrafficRepository;
import com.greentraffic.infrastructure.influxdb.client.InfluxDbClientProvider;
import com.greentraffic.infrastructure.influxdb.config.InfluxDbProperties;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InfluxTrafficRepository implements TrafficRepository {

    private final InfluxDBClient client;
    private final String bucket;

    public InfluxTrafficRepository(
            InfluxDbClientProvider clientProvider,
            InfluxDbProperties properties) {

        this.client = clientProvider.getClient();
        this.bucket = properties.getBucket();
    }

    @Override
    public void save(TrafficMetric metric) {

        Point point = Point
                .measurement("traffic_flow")
                .addTag("road_id", metric.roadId())
                .addTag("direction", metric.direction())
                .addField("vehicle_count", metric.trafficFlow())
                .addField("speed", metric.averageSpeed())
                .time(metric.timestamp(), WritePrecision.S);

        WriteApiBlocking writeApi = client.getWriteApiBlocking();

        writeApi.writePoint(point);
    }

    @Override
    public List<TrafficMetric> query(
            Instant start,
            Instant stop) {

        String flux = """
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) =>
                      r._measurement == "traffic_flow"
                  )
                """;

        String query = flux.formatted(
                bucket,
                start,
                stop
        );

        List<FluxTable> tables =
            client.getQueryApi().query(query);

        List<TrafficMetric> result = new ArrayList<>();

        // 遍历表和记录，构建结果
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {

            String roadId =
                    (String) record.getValueByKey("road_id");

            String direction =
                    (String) record.getValueByKey("direction");

            Object value = record.getValue();

            if (value == null) {
                continue;
            }

            Integer vehicleCount = null;
            Double speed = null;
            Double co2 = null;

            String field = record.getField();
            if ("vehicle_count".equals(field)) {
                vehicleCount = ((Number) value).intValue();
            } else if ("speed".equals(field)) {
                speed = ((Number) value).doubleValue();
            } else if ("co2_emission".equals(field)) {
                co2 = ((Number) value).doubleValue();
            }

            // map to TrafficMetric: vehicleCount -> trafficFlow, speed -> averageSpeed, co2 -> co2Emission
            result.add(new TrafficMetric(
                    roadId,
                    direction,
                    null,
                    vehicleCount,
                    speed,
                    co2,
                    null,
                    record.getTime()
            ));
            }
        }

        return result;
    }
}
