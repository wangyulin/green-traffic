可以。既然 **`InfluxDbProperties` + `InfluxDbClientProvider` 已经完成**，下面只做“最小可用链路”：

> **写入 → 查询 → Repository → Service 调用 → Spring Boot 启动验证**

## 1. 最终目录

```text
green-traffic-core
└── src/main/java/com/xxx/greentraffic/core
    └── repository
        └── TrafficRepository.java

green-traffic-infrastructure
└── src/main/java/com/xxx/greentraffic/infrastructure
    └── influxdb
        └── repository
            └── InfluxTrafficRepository.java

green-traffic-api
└── src/main/java/com/xxx/greentraffic/api
    └── controller
        └── InfluxTestController.java
```

依赖：

```text
green-traffic-api
 ├── green-traffic-core
 └── green-traffic-infrastructure
                  │
                  └── green-traffic-core
```

---

# 2. Core：定义 Repository 接口

位置：

```text
green-traffic-core/src/main/java/com/xxx/greentraffic/core/repository/TrafficRepository.java
```

```java
package com.xxx.greentraffic.core.repository;

import java.time.Instant;
import java.util.List;

public interface TrafficRepository {

    void save(TrafficData data);

    List<TrafficData> query(Instant start, Instant stop);
}
```

如果你现在还没有 `TrafficData`，先简单定义：

```text
green-traffic-core
└── domain
    └── TrafficData.java
```

```java
package com.xxx.greentraffic.core.domain;

import java.time.Instant;

public record TrafficData(
        String roadId,
        String direction,
        Integer vehicleCount,
        Double speed,
        Instant time
) {
}
```

---

# 3. Infrastructure：实现 InfluxDB Repository

位置：

```text
green-traffic-infrastructure/src/main/java/com/xxx/greentraffic/infrastructure/influxdb/repository/InfluxTrafficRepository.java
```

```java
package com.xxx.greentraffic.infrastructure.influxdb.repository;

import com.xxx.greentraffic.core.domain.TrafficData;
import com.xxx.greentraffic.core.repository.TrafficRepository;
import com.xxx.greentraffic.infrastructure.influxdb.client.InfluxDbClientProvider;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InfluxTrafficRepository implements TrafficRepository {

    private final InfluxDBClient client;

    public InfluxTrafficRepository(
            InfluxDbClientProvider clientProvider) {

        this.client = clientProvider.getClient();
    }

    @Override
    public void save(TrafficData data) {

        Point point = Point
                .measurement("traffic_flow")
                .addTag("road_id", data.roadId())
                .addTag("direction", data.direction())
                .addField("vehicle_count", data.vehicleCount())
                .addField("speed", data.speed())
                .time(data.time(), WritePrecision.S);

        WriteApiBlocking writeApi = client.getWriteApiBlocking();

        writeApi.writePoint(point);
    }

    @Override
    public List<TrafficData> query(
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
                client.getOptions().getBucket(),
                start,
                stop
        );

        List<FluxRecord> records =
                client.getQueryApi().query(query);

        List<TrafficData> result = new ArrayList<>();

        // 后续实际项目建议在这里做完整字段聚合/映射
        for (FluxRecord record : records) {

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

            if ("vehicle_count".equals(record.getField())) {
                vehicleCount = ((Number) value).intValue();
            }

            if ("speed".equals(record.getField())) {
                speed = ((Number) value).doubleValue();
            }

            result.add(new TrafficData(
                    roadId,
                    direction,
                    vehicleCount,
                    speed,
                    record.getTime()
            ));
        }

        return result;
    }
}
```

### 这里有一个实际项目要注意的点

InfluxDB 的：

```text
vehicle_count
speed
```

是两个 field，所以一次查询会得到两条 Flux Record。

**生产代码不要直接按照上面这样返回 `TrafficData`，而应该做一次聚合映射。**

但是为了先验证 InfluxDB **能成功读写**，这个版本足够。

---

# 4. API：做一个最简单测试接口

位置：

```text
green-traffic-api/src/main/java/com/xxx/greentraffic/api/controller/InfluxTestController.java
```

```java
package com.xxx.greentraffic.api.controller;

import com.xxx.greentraffic.core.domain.TrafficData;
import com.xxx.greentraffic.core.repository.TrafficRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/test/influx")
public class InfluxTestController {

    private final TrafficRepository repository;

    public InfluxTestController(TrafficRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/write")
    public String write() {

        TrafficData data = new TrafficData(
                "ROAD-001",
                "EAST",
                120,
                42.5,
                Instant.now()
        );

        repository.save(data);

        return "OK";
    }

    @GetMapping("/read")
    public List<TrafficData> read() {

        Instant stop = Instant.now();
        Instant start = stop.minusSeconds(3600);

        return repository.query(start, stop);
    }
}
```

---

# 5. 一个关键问题：`getBucket()` 不一定这样取

如果你前面的 `InfluxDbClientProvider` 是按照我之前给你的方式：

```java
InfluxDBClientFactory.create(
    properties.getUrl(),
    properties.getToken().toCharArray(),
    properties.getOrg(),
    properties.getBucket()
);
```

那么为了避免依赖 Client 内部配置，**更推荐 Provider 同时暴露 Properties**。

例如：

```java
@Component
public class InfluxDbClientProvider {

    private final InfluxDBClient client;
    private final InfluxDbProperties properties;

    public InfluxDbClientProvider(
            InfluxDbProperties properties) {

        this.properties = properties;

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

    public InfluxDbProperties getProperties() {
        return properties;
    }
}
```

然后 Repository：

```java
String bucket =
        clientProvider.getProperties().getBucket();
```

比：

```java
client.getOptions().getBucket()
```

更清晰。

---

# 6. 最后验证

启动 Spring Boot：

```bash
mvn clean package
```

启动：

```bash
java -jar green-traffic-api.jar
```

写入：

```bash
curl -X POST http://localhost:8080/test/influx/write
```

返回：

```text
OK
```

然后查询：

```bash
curl http://localhost:8080/test/influx/read
```

能看到刚才写进去的数据，就说明：

```text
Spring Boot
    ↓
Controller
    ↓
TrafficRepository
    ↓
InfluxTrafficRepository
    ↓
InfluxDbClientProvider
    ↓
InfluxDB
```

**整个链路已经打通。**

下一步不要继续堆代码，建议马上把 `InfluxTrafficRepository` 的**查询结果映射和 Point 的 Tag/Field 设计**定下来——这会直接决定后面交通流量、速度、拥堵指数、车辆轨迹等时序数据是否好查。
