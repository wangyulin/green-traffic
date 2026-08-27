可以。基于你现在这个多模块 Maven 结构，我建议**不要把 InfluxDB 代码直接塞进 `green-traffic-core`**，而是把“基础设施适配”放到 `green-traffic-infrastructure`，业务层只依赖抽象接口。

Spring Boot 4.0.7 官方文档目前仍提供该版本；Boot 4 要求 Java 17+，Maven 建议使用依赖管理而不是到处手写版本。([Home][1])

## 1. 先确定模块职责

你现在：

```text
green-traffic
├── green-traffic-api
├── green-traffic-common
├── green-traffic-core
├── green-traffic-dashboard
├── green-traffic-doc
├── green-traffic-infrastructure   ← InfluxDB 放这里
├── green-traffic-model
├── green-traffic-push
├── green-traffic-simulator
└── pom.xml
```

建议形成这个依赖关系：

```text
                 ┌──────────────────────┐
                 │ green-traffic-model  │
                 │ DTO / Entity / VO     │
                 └──────────┬───────────┘
                            │
                 ┌──────────▼───────────┐
                 │  green-traffic-core  │
                 │ 业务接口 / Service    │
                 └──────────┬───────────┘
                            │
                 ┌──────────▼────────────────┐
                 │ green-traffic-infrastructure│
                 │ InfluxDB / MySQL / Redis    │
                 │ 第三方基础设施适配           │
                 └─────────────────────────────┘
                            ▲
                            │
                 ┌──────────┴───────────┐
                 │ green-traffic-api    │
                 │ Spring Boot 启动应用  │
                 └──────────────────────┘
```

**核心原则：**

> `core` 不知道 InfluxDB 是什么，`infrastructure` 才知道。

这样以后你从：

```text
InfluxDB
```

换成：

```text
TimescaleDB
ClickHouse
TDengine
国产时序数据库
```

业务代码基本不用动。

---

# 2. 推荐最终目录

重点开发：

```text
green-traffic-infrastructure
└── src
    └── main
        ├── java
        │   └── com.xxx.greentraffic
        │       └── infrastructure
        │           └── influxdb
        │               ├── config
        │               │   └── InfluxDbProperties.java
        │               ├── client
        │               │   └── InfluxDbClientFactory.java
        │               ├── repository
        │               │   └── InfluxTrafficRepository.java
        │               └── repository
        │                   └── impl
        │                       └── InfluxTrafficRepositoryImpl.java
        │
        └── resources
            ├── application.yml
            ├── application-dev.yml
            └── application-prod.yml
```

不过我更推荐稍微整理成：

```text
infrastructure
└── influxdb
    ├── config
    │   └── InfluxDbProperties.java
    ├── client
    │   └── InfluxDbClientFactory.java
    ├── repository
    │   └── InfluxTrafficRepositoryImpl.java
    └── mapper
        └── TrafficPointMapper.java
```

---

# 3. Maven：依赖到底放哪里？

这是最重要的。

## 根 pom.xml

根 `pom.xml` 只负责：

* Spring Boot 版本
* Java 版本
* 公共依赖版本
* modules

例如：

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>4.0.7</spring-boot.version>
    <influxdb-client.version>7.3.0</influxdb-client.version>
</properties>
```

Spring Boot 官方建议使用其 dependency management，由 Boot 统一管理兼容依赖。([Home][2])

---

# 4. InfluxDB 依赖放 infrastructure

`green-traffic-infrastructure/pom.xml`：

```xml
<dependencies>

    <!-- Spring Boot 基础 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <!-- InfluxDB Java Client -->
    <dependency>
        <groupId>com.influxdb</groupId>
        <artifactId>influxdb-client-java</artifactId>
        <version>${influxdb-client.version}</version>
    </dependency>

</dependencies>
```

InfluxDB 官方提供 Java Client，用于 InfluxDB API 的访问。([InfluxData][3])

**不要把 `influxdb-client-java` 放到：**

```text
green-traffic-common
green-traffic-model
green-traffic-core
```

否则基础设施污染业务层。

---

# 5. 配置不要写死，考虑跨平台

这是你这个项目现在最值得提前设计的地方。

不要：

```java
InfluxDBClientFactory.create(
    "http://192.168.1.100:8086",
    "xxxxx",
    "traffic"
);
```

更不要：

```java
String url = "http://localhost:8086";
```

应该全部配置化。

---

# 6. application.yml

放：

```text
green-traffic-api/src/main/resources/application.yml
```

或者如果你的启动模块是 infrastructure，则放启动模块。

推荐：

```yaml
spring:
  application:
    name: green-traffic

traffic:
  influxdb:
    enabled: true
    url: ${INFLUXDB_URL:http://localhost:8086}
    token: ${INFLUXDB_TOKEN:}
    org: ${INFLUXDB_ORG:green-traffic}
    bucket: ${INFLUXDB_BUCKET:traffic}
    connect-timeout: 10s
    read-timeout: 30s
    write-timeout: 10s
```

这里最关键的是：

```yaml
${INFLUXDB_URL:http://localhost:8086}
```

意味着：

**Linux：**

```bash
export INFLUXDB_URL=http://10.10.1.100:8086
```

**Windows：**

```powershell
$env:INFLUXDB_URL="http://10.10.1.100:8086"
```

**Docker：**

```yaml
environment:
  INFLUXDB_URL: http://influxdb:8086
```

**Kubernetes：**

```yaml
env:
  - name: INFLUXDB_URL
    value: http://influxdb:8086
```

Java 代码完全不用改。

这才是真正的**跨平台**。

---

# 7. 配置类

创建：

```text
infrastructure/influxdb/config/InfluxDbProperties.java
```

```java
package com.xxx.greentraffic.infrastructure.influxdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "traffic.influxdb")
public class InfluxDbProperties {

    private boolean enabled;

    private String url;

    private String token;

    private String org;

    private String bucket;

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration readTimeout = Duration.ofSeconds(30);

    private Duration writeTimeout = Duration.ofSeconds(10);

    // getter / setter
}
```

---

# 8. 创建 InfluxDB Client

```text
infrastructure/influxdb/client/InfluxDbClientFactory.java
```

```java
package com.xxx.greentraffic.infrastructure.influxdb.client;

import com.xxx.greentraffic.infrastructure.influxdb.config.InfluxDbProperties;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.stereotype.Component;

@Component
public class InfluxDbClientFactory {

    private final InfluxDBClient client;

    public InfluxDbClientFactory(InfluxDbProperties properties) {

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
```

不过这里建议你**不要真的叫 Factory**，因为 Spring Bean 本身已经是单例管理。

更简单：

```java
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
```

---

# 9. 开启 ConfigurationProperties

启动类：

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class GreenTrafficApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenTrafficApplication.class, args);
    }
}
```

这样：

```java
@ConfigurationProperties(prefix = "traffic.influxdb")
```

就会自动生效。

---

# 10. 不要让 Core 直接操作 InfluxDB

这是整个架构的关键。

在：

```text
green-traffic-core
```

定义接口：

```text
core
└── repository
    └── TrafficRepository.java
```

例如：

```java
public interface TrafficRepository {

    void save(TrafficData data);

    List<TrafficData> query(
            Instant start,
            Instant end
    );
}
```

`core` 只知道：

```java
TrafficRepository
```

不知道：

```java
InfluxDBClient
Point
FluxTable
FluxRecord
```

---

# 11. infrastructure 实现这个接口

```java
@Repository
public class InfluxTrafficRepository
        implements TrafficRepository {

    private final InfluxDBClient client;

    public InfluxTrafficRepository(
            InfluxDbClientProvider provider) {

        this.client = provider.getClient();
    }

    @Override
    public void save(TrafficData data) {

        // Point
        // writeApi
        // 写入 InfluxDB
    }

    @Override
    public List<TrafficData> query(
            Instant start,
            Instant end) {

        // Flux 查询
        return List.of();
    }
}
```

于是形成：

```text
业务 Service
     ↓
TrafficRepository
     ↓
InfluxTrafficRepository
     ↓
InfluxDB
```

而不是：

```text
业务 Service
     ↓
InfluxDBClient
     ↓
InfluxDB
```

---

# 12. InfluxDB 写入示例

例如交通流量：

```java
Point point = Point
        .measurement("traffic_flow")
        .addTag("road_id", data.getRoadId())
        .addTag("direction", data.getDirection())
        .addField("vehicle_count", data.getVehicleCount())
        .addField("speed", data.getSpeed())
        .time(Instant.now(), WritePrecision.S);

client
        .getWriteApiBlocking()
        .writePoint(point);
```

最终 InfluxDB 类似：

```text
measurement
    traffic_flow

tags
    road_id=R001
    direction=EAST

fields
    vehicle_count=125
    speed=42.5

<!-- 注：`green-traffic-model` 模块已移除；文中对其的历史引用保留为参考，实际模型已迁移到 `green-traffic-core` 或 `green-traffic-common` -->


    2026-08-20T15:00:00Z
```

---

# 13. 跨平台真正应该考虑的东西

我建议你现在就统一这几个原则：

| 项目            | 不推荐                | 推荐                |
| ------------- | ------------------ | ----------------- |
| IP            | 写死                 | 环境变量              |
| 端口            | 写死                 | 配置                |
| Token         | Git                | 环境变量/Secret       |
| 文件路径          | `/opt/xxx`         | `Path` + 配置       |
| Windows/Linux | `\` `/`            | `Path.of()`       |
| Docker        | localhost          | service name      |
| Kubernetes    | application.yml 写死 | ConfigMap/Secret  |
| 数据库           | 业务代码创建             | infrastructure    |
| InfluxDB      | core 依赖            | infrastructure 依赖 |
| 时间            | LocalDateTime      | Instant           |
| 时区            | 系统默认               | UTC               |

特别注意：

### 时间统一使用 `Instant`

不要在时序数据里面大量使用：

```java
LocalDateTime
```

推荐：

```java
Instant
```

因为：

```text
Mac
Windows
Linux
Docker
Kubernetes
```

可能存在不同系统时区。

交通系统这种项目，时间是核心数据，**建议数据库统一 UTC，展示层再转换北京时间/日本时间。**

---

# 14. 你的 Maven 模块我建议最终这样调整

```text
green-traffic
│
├── pom.xml
│
├── green-traffic-model
│   └── DTO / VO / Domain Object
│
├── green-traffic-common
│   └── 通用工具 / Result / Exception
│
├── green-traffic-core
│   ├── service
│   └── repository
│       └── TrafficRepository.java
│
├── green-traffic-infrastructure
│   ├── influxdb
│   │   ├── config
│   │   ├── client
│   │   └── repository
│   │
│   ├── mysql
│   ├── redis
│   └── mqtt
│
├── green-traffic-api
│   ├── controller
│   └── GreenTrafficApplication.java
│
├── green-traffic-push
│
├── green-traffic-simulator
│
├── green-traffic-dashboard
│
└── green-traffic-doc
```

最终依赖关系：

```text
api
 │
 ├── core
 │    │
 │    ├── model
 │    └── common
 │
 └── infrastructure
      │
      ├── core
      ├── influxdb-client
      ├── mysql-driver
      ├── redis-client
      └── mqtt-client
```

---

## 15. 你现在直接照着做

**第一步**

根 `pom.xml`：

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>4.0.7</spring-boot.version>
    <influxdb-client.version>7.3.0</influxdb-client.version>
</properties>
```

**第二步**

`green-traffic-infrastructure/pom.xml` 加：

```xml
<dependency>
    <groupId>com.influxdb</groupId>
    <artifactId>influxdb-client-java</artifactId>
    <version>${influxdb-client.version}</version>
</dependency>
```

**第三步**

建立：

```text
infrastructure/influxdb/
├── config/
│   └── InfluxDbProperties.java
├── client/
│   └── InfluxDbClientProvider.java
└── repository/
    └── InfluxTrafficRepository.java
```

**第四步**

`application.yml`：

```yaml
traffic:
  influxdb:
    enabled: true
    url: ${INFLUXDB_URL:http://localhost:8086}
    token: ${INFLUXDB_TOKEN:}
    org: ${INFLUXDB_ORG:green-traffic}
    bucket: ${INFLUXDB_BUCKET:traffic}
```

**第五步**

`core` 定义：

```java
TrafficRepository
```

**第六步**

`infrastructure` 实现：

```java
InfluxTrafficRepository
```

**第七步**

业务代码永远只调用：

```java
TrafficRepository
```

不要出现：

```java
InfluxDBClient
```

---

### 一句话定架构

> **`core` 定义“我要存交通数据”，`infrastructure` 决定“用 InfluxDB 怎么存”，`api` 决定“怎么提供 HTTP 服务”，配置通过环境变量解决 Windows/Linux/Docker/K8s 差异。**

这套设计对于你后面继续接 **MySQL + Redis + MQTT + Kafka + InfluxDB** 也能直接复用。

[1]: https://docs.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com "System Requirements :: Spring Boot"
[2]: https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html?utm_source=chatgpt.com "Build Systems :: Spring Boot"
[3]: https://docs.influxdata.com/influxdb/v1/tools/api_client_libraries/?utm_source=chatgpt.com "InfluxDB client libraries | InfluxDB OSS v1 Documentation"
