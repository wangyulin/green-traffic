package com.greentraffic.infrastructure.influxdb.client;

import com.greentraffic.infrastructure.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.stereotype.Component;

@Component
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
}
