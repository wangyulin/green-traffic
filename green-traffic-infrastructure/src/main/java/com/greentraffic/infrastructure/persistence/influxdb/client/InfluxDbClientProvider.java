package com.greentraffic.infrastructure.persistence.influxdb.client;

import com.greentraffic.infrastructure.persistence.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(prefix = "traffic.storage", name = "type", havingValue = "influx")
public class InfluxDbClientProvider {

    private final InfluxDBClient client;

    public InfluxDbClientProvider(InfluxDbProperties properties) {

        this.client = InfluxDBClientFactory.create(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getOrg(),
                properties.getBucket()
        );
    }

    public InfluxDBClient getClient() {
        return client;
    }

    @PreDestroy
    public void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            // log at warn if logger present; keep silent to avoid startup failure
        }
    }
}
