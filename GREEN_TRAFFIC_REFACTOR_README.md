# GreenTraffic 架构与代码改造实施指南

> 本文档针对 `wangyulin/green-traffic` 当前 `main` 分支进行代码级检查，并按照后续目标技术栈重新规划。
>
> 目标技术栈：
> - Java 17
> - Spring Boot 4.0.x
> - Spring Web
> - Spring Validation
> - Spring WebSocket
> - MyBatis Spring Boot Starter 4.0.x
> - MySQL 8.x
> - RocketMQ Spring
> - Spring Events
> - InfluxDB
> - VictoriaMetrics

---

## 1. 先说结论

这个项目**不是需要推倒重写**。

目前最值得保留的部分是：

1. Maven 多模块结构；
2. `core` 中已经出现的 `application / port / input / output` 分层；
3. Infrastructure Adapter 思路；
4. 消息统一模型；
5. 业务数据与基础设施实现通过 Port 解耦；
6. WebSocket、消息、时序数据存储都具备独立模块的演进空间。

真正需要重点整改的是：

| 优先级 | 问题 | 影响 |
|---|---|---|
| P0 | Spring Boot 4.0.7 + MyBatis Spring Boot 3.0.3 | **版本不兼容，必须先改** |
| P0 | `green-traffic-push` 当前只有 `pom.xml` | WebSocket 推送模块实际上没有形成完整实现 |
| P0 | VictoriaMetrics Adapter 的查询模型不完整 | 写入了多个指标，查询却只还原 `trafficFlow` |
| P0 | VictoriaMetrics 与 InfluxDB 的职责混乱 | 后续查询、指标模型、运维成本都会变复杂 |
| P1 | Infrastructure POM 塞入大量不必要依赖 | 模块边界被污染 |
| P1 | RocketMQ、Spring Events 同时承担消息能力，但职责没有明确 | 容易出现重复消费、链路混乱 |
| P1 | 消息订阅接口的 `unsubscribe` 参数语义不统一 | 生命周期管理容易出 bug |
| P1 | 时序数据内存队列无容量上限，失败后有丢数据风险 | 生产环境可靠性不足 |
| P1 | API 中存在 `HelloController`、测试 Controller 等非正式接口 | 需要清理 |
| P2 | `spring-cloud.version` 当前没有实际价值 | 应删除 |
| P2 | `H2` 作为运行时依赖不适合长期生产结构 | 建议只用于测试 Profile |
| P2 | 大量手写 `RestTemplate` | 建议统一成 HTTP Client / WebClient，并集中配置连接池、超时、重试 |
| P2 | 缺少完整的测试、数据库迁移、配置分环境、可观测性闭环 | 后续维护成本高 |

---

# 2. 当前项目实际结构

当前根 POM 已经是多模块 Maven 项目：

```text
green-traffic
├── green-traffic-common
├── green-traffic-model
├── green-traffic-simulator
├── green-traffic-core
├── green-traffic-push
├── green-traffic-api
└── green-traffic-infrastructure
```

当前根 POM 的 Java 版本为 17，Spring Boot 为 4.0.7，但 MyBatis Spring Boot Starter 仍然配置为 3.0.3。

这就是第一个必须处理的问题。

Spring Boot 4 要求 Spring Framework 7，MyBatis 官方给出的 MyBatis Spring Boot Starter 4.0.x 才对应 Spring Boot 4.0 + Java 17。因此：

```xml
<mybatis-spring-boot.version>3.0.3</mybatis-spring-boot.version>
```

必须升级到：

```xml
<mybatis-spring-boot.version>4.0.x</mybatis-spring-boot.version>
```

建议直接使用当前 4.0.x 稳定版本，不要使用 3.0.x。

---

# 3. 推荐最终架构

不要把项目改成微服务。

当前业务量和系统定位更适合：

> **模块化单体 Modular Monolith + 六边形/端口适配器架构**

最终建议：

```text
                         ┌──────────────────────────┐
                         │        Dashboard         │
                         │ HTML / ECharts / WS      │
                         └────────────┬─────────────┘
                                      │
                         ┌────────────▼─────────────┐
                         │       green-traffic-api  │
                         │ REST / WebSocket / DTO   │
                         └────────────┬─────────────┘
                                      │ Input Port
                    ┌─────────────────▼──────────────────┐
                    │         green-traffic-core        │
                    │                                    │
                    │ Application Service                │
                    │ Domain / Rule / Carbon Calculation │
                    │ Input Port / Output Port            │
                    └──────┬───────────────┬─────────────┘
                           │               │
                    Output Port       Output Port
                           │               │
              ┌────────────▼───┐    ┌────▼────────────────┐
              │ Infrastructure  │    │ Infrastructure      │
              │ MySQL / MyBatis │    │ MQ / TimeSeries     │
              └───────┬─────────┘    └──────────┬─────────┘
                      │                         │
                  ┌───▼───┐              ┌──────▼──────┐
                  │ MySQL │              │ InfluxDB    │
                  └───────┘              └─────────────┘

                         技术/系统监控指标
                               │
                         Micrometer
                               │
                               ▼
                       VictoriaMetrics
```

这里有一个非常重要的职责划分：

## InfluxDB

用于：

- 交通流量
- 平均速度
- 等待时间
- CO2 排放
- SUMO 仿真指标
- 路口/道路业务时序数据

也就是：

> **业务时间序列**

## VictoriaMetrics

用于：

- JVM
- HTTP 请求
- WebSocket 连接
- RocketMQ 消费
- 消息处理耗时
- 数据库连接池
- 应用 CPU / Memory
- 自定义系统监控指标

也就是：

> **技术监控指标**

不要再把业务交通数据和技术监控指标混成一套 Metric。

---

# 4. 第一阶段：先建立安全改造分支

不要直接修改 main。

```bash
git checkout main
git pull

git checkout -b refactor/boot4-mybatis4-timeseries
```

然后打一个基线：

```bash
mvn clean test
```

如果现在已经不能通过，不要急着修改业务代码。

先记录：

```bash
mvn -version
java -version
```

然后：

```bash
mvn clean verify -DskipTests
```

把错误保存下来：

```bash
mvn clean verify -DskipTests > build-before-refactor.log 2>&1
```

---

# 5. 第二阶段：修改根 POM

打开：

```text
pom.xml
```

## 5.1 Java

保持：

```xml
<java.version>17</java.version>
```

## 5.2 Spring Boot

保持：

```xml
<spring-boot.version>4.0.7</spring-boot.version>
```

如果以后升级，建议统一升级到最新 4.0.x 维护版本，不要直接跨到其他大版本。

## 5.3 MyBatis

把：

```xml
<mybatis-spring-boot.version>3.0.3</mybatis-spring-boot.version>
```

改成：

```xml
<mybatis-spring-boot.version>4.0.x</mybatis-spring-boot.version>
```

然后：

```bash
mvn -U clean verify -DskipTests
```

如果这里出现 MyBatis API 错误，再逐个处理，不要一次修改几十个文件。

---

# 6. 第三阶段：删除没有实际用途的 Spring Cloud 版本

根 POM 当前存在：

```xml
<spring-cloud.version>2025.0.0</spring-cloud.version>
```

但当前项目不是 Spring Cloud 微服务项目。

如果没有：

- Gateway
- OpenFeign
- Nacos
- Config Server
- LoadBalancer
- CircuitBreaker

就删除：

```xml
<spring-cloud.version>2025.0.0</spring-cloud.version>
```

不要为了“看起来高级”而保留 Spring Cloud。

---

# 7. 第四阶段：重新定义 Maven 模块职责

建议最终变成：

```text
green-traffic
│
├── green-traffic-common
│   └── 极少量公共基础类型
│
├── green-traffic-model
│   └── DTO / Command / Query / View
│
├── green-traffic-core
│   └── 业务核心
│
├── green-traffic-infrastructure
│   └── MyBatis / RocketMQ / Spring Event / InfluxDB / VM
│
├── green-traffic-api
│   └── REST / WebSocket / Application 启动入口
│
├── green-traffic-simulator
│   └── 模拟数据源
│
└── green-traffic-dashboard
    └── 前端静态页面
```

如果 `green-traffic-push` 最终只是 WebSocket 推送，建议删除它，把 WebSocket 放到 `api`：

```text
api
├── controller
├── websocket
└── GreenTrafficApplication
```

原因很简单：

> WebSocket 是系统对外的接口适配器，不应该成为一个空模块。

如果未来 WebSocket 会独立部署，再重新拆成模块。

---

# 8. 第五阶段：清理 Infrastructure POM

当前：

```text
green-traffic-infrastructure/pom.xml
```

依赖明显偏多。

尤其是同时出现：

```xml
spring-boot-starter-actuator
spring-boot-actuator
spring-boot-health
spring-boot
spring-boot-starter
spring-boot-starter-web
spring-boot-starter-jdbc
```

这不是一个好的生产模块依赖结构。

建议按实际用途保留。

基础设施模块需要什么，就引入什么。

例如：

```xml
<dependencies>

    <dependency>
        <groupId>com.greentraffic</groupId>
        <artifactId>green-traffic-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>com.influxdb</groupId>
        <artifactId>influxdb-client-java</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>
```

不要把整个 Spring Boot 都塞进 Infrastructure。

---

# 9. 第六阶段：MySQL 和 MyBatis 的正确职责

MySQL 不负责高频交通时序数据。

MySQL 建议保存：

```text
intersection
road
traffic_rule
optimization_advice
alarm
system_config
```

例如：

```text
intersection
├── id
├── code
├── name
├── district
├── longitude
├── latitude
└── status
```

MySQL 主要保存：

> “是什么、属于谁、当前状态是什么、规则是什么”

而 InfluxDB 保存：

> “什么时候发生了什么”

---

# 10. MyBatis 目录建议

Infrastructure 中建议：

```text
persistence
└── mysql
    ├── mapper
    │   ├── IntersectionMapper.java
    │   └── OptimizationAdviceMapper.java
    │
    ├── entity
    │   ├── IntersectionDO.java
    │   └── OptimizationAdviceDO.java
    │
    └── adapter
        ├── IntersectionRepositoryAdapter.java
        └── OptimizationAdviceRepositoryAdapter.java
```

核心层不要出现：

```java
@Mapper
```

也不要出现：

```java
SqlSession
JdbcTemplate
DataSource
```

Core 只依赖：

```java
public interface IntersectionRepositoryPort {
    Optional<Intersection> findByCode(String code);
}
```

Infrastructure 再实现：

```java
@Component
public class IntersectionRepositoryAdapter
        implements IntersectionRepositoryPort {

    private final IntersectionMapper mapper;

    public IntersectionRepositoryAdapter(IntersectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Intersection> findByCode(String code) {
        return Optional.ofNullable(mapper.selectByCode(code))
                .map(this::toDomain);
    }
}
```

这才是真正的六边形架构。

---

# 11. 第七阶段：解决当前 VictoriaMetricAdapter 最大的问题

当前代码的一个核心问题：

写入时包含：

```text
trafficFlow
averageSpeed
co2Emission
```

但查询时只把：

```text
sample
```

转换成：

```java
trafficFlow
```

其他字段直接：

```java
null
```

这意味着：

> “写入的数据”和“查询出来的数据”不是同一个完整的数据模型。

这是必须修改的。

---

# 12. 推荐的 InfluxDB 数据模型

交通业务数据建议一个 measurement：

```text
traffic_metric
```

Tags：

```text
roadId
direction
vehicleType
location
```

Fields：

```text
trafficFlow
averageSpeed
co2Emission
averageWaitingTime
vehicleCount
```

时间：

```text
timestamp
```

例如：

```text
traffic_metric,
roadId=R001,
direction=NORTH,
vehicleType=CAR,
location=A01
trafficFlow=32i,
averageSpeed=38.5,
co2Emission=12.34,
averageWaitingTime=18.5
```

注意：

> 高频变化的数据尽量作为 field，不要全部做 tag。

否则 VictoriaMetrics / InfluxDB 都可能产生严重的高基数问题。

---

# 13. VictoriaMetrics 应该怎么使用

推荐：

```text
Spring Boot
    │
    │ Micrometer
    ▼
/actuator/prometheus
    │
    ▼
vmagent / Prometheus
    │
    │ remote_write
    ▼
VictoriaMetrics
```

例如：

```text
http_server_requests_seconds
jvm_memory_used_bytes
jvm_threads_live
process_cpu_usage
rocketmq_consume_latency
traffic_websocket_connections
```

不要再自己实现：

```java
RestTemplate.postForEntity(...)
```

去手写 JVM 监控。

---

# 14. 第八阶段：修改 VictoriaMetrics Adapter 的设计

当前 Adapter 自己：

```java
new RestTemplate()
```

这是不推荐的。

改成 Spring 管理：

```java
@Bean
RestClient metricsRestClient(RestClient.Builder builder) {
    return builder
            .baseUrl(metricsProperties.getVmUrl())
            .build();
}
```

然后：

```java
@Service
public class VictoriaMetricsAdapter {

    private final RestClient restClient;

    public VictoriaMetricsAdapter(RestClient restClient) {
        this.restClient = restClient;
    }
}
```

这样：

- 超时
- 连接池
- Header
- 鉴权
- 日志
- 重试

都可以统一配置。

---

# 15. 第九阶段：不要使用无限内存队列

当前：

```java
new LinkedBlockingQueue<>()
```

默认是近似无限容量。

如果 VictoriaMetrics / InfluxDB 挂掉：

```text
业务持续产生数据
        ↓
队列不断增长
        ↓
JVM 内存不断增加
        ↓
最终 OOM
```

必须增加上限。

例如：

```java
new ArrayBlockingQueue<>(100_000);
```

并定义：

```text
queueFullPolicy = DROP / BLOCK / LOCAL_FILE
```

生产建议：

```text
正常：批量发送
短暂失败：指数退避
持续失败：本地落盘
恢复后：重新发送
超过容量：告警
```

---

# 16. 第十阶段：不要在 flush 中 Thread.sleep

当前代码在发送失败后：

```java
Thread.sleep(delay);
```

这会阻塞 flush 线程。

更合理的是：

```text
发送失败
   ↓
记录失败
   ↓
进入 retry queue
   ↓
ScheduledExecutor 定时重试
```

不要让一次网络异常把整个数据发送线程卡住。

---

# 17. 第十一阶段：RocketMQ 和 Spring Events 的职责必须分开

建议明确：

## Spring Events

用于：

```text
同一个 JVM 内
模块之间
低延迟
非可靠业务通知
```

例如：

```text
TrafficCalculatedEvent
OptimizationGeneratedEvent
WebSocketPushEvent
```

特点：

> 本地事件，不保证系统重启后还能继续消费。

## RocketMQ

用于：

```text
跨进程
可靠异步
削峰
重试
最终一致性
```

例如：

```text
traffic.metric.received
traffic.metric.processed
traffic.alert.generated
```

特点：

> 需要持久化、重试、消费进度。

---

# 18. 推荐消息链路

最终建议：

```text
Simulator
    │
    ▼
RocketMQ
    │
    ▼
TrafficMetricConsumer
    │
    ▼
Core Application Service
    │
    ├──> InfluxDB
    │
    ├──> Rule Engine
    │
    └──> Spring Event
              │
              ▼
       WebSocket Publisher
              │
              ▼
          Dashboard
```

这比：

```text
Simulator
   ↓
Spring Event
   ↓
Core
   ↓
Spring Event
   ↓
Push
```

更适合以后真正接入真实交通设备。

---

# 19. 消息必须增加幂等设计

每条消息必须有：

```text
messageId
eventId
timestamp
source
schemaVersion
```

当前 Message 已经有不少这些字段，这是正确方向。

建议增加：

```text
producer
retryCount
```

数据库或业务层至少保证：

```text
eventId 唯一
```

处理逻辑：

```java
if (processedEventRepository.exists(eventId)) {
    return;
}

process();

processedEventRepository.save(eventId);
```

否则 RocketMQ 重投后可能重复写入。

---

# 20. 消息 Schema 不要直接绑定 Java Bean

不要让 RocketMQ 消息长期依赖：

```java
TrafficMetric
```

推荐：

```text
TrafficMetricMessageV1
TrafficMetricMessageV2
```

消息是外部契约。

领域对象是内部模型。

两者之间：

```text
Message
  ↓
MessageConverter
  ↓
Command
  ↓
Domain
```

这样以后字段变更不会直接打爆消费者。

---

# 21. 第十二阶段：修正 MessageSubscriber

当前：

```java
void unsubscribe(String subscriptionId);
```

但是：

```java
subscribe(String messageType, Consumer<Message<?>> handler);
```

返回值却是：

```java
void
```

这会导致订阅 ID 不明确。

建议：

```java
Subscription subscribe(
    String messageType,
    Consumer<Message<?>> handler
);
```

然后：

```java
subscription.close();
```

这是更自然的生命周期模型。

---

# 22. 第十三阶段：WebSocket 正确实现

建议：

```text
api
└── websocket
    ├── TrafficWebSocketHandler.java
    ├── WebSocketSessionManager.java
    ├── WebSocketMessagePublisher.java
    └── WebSocketConfig.java
```

Session Manager：

```java
@Component
public class WebSocketSessionManager {

    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    public void add(WebSocketSession session) {
        sessions.add(session);
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session);
    }

    public Set<WebSocketSession> sessions() {
        return Set.copyOf(sessions);
    }
}
```

不要把 session 放到 Controller 静态变量中。

---

# 23. WebSocket 推送必须考虑慢客户端

不能：

```java
sessions.forEach(session -> session.sendMessage(...));
```

然后假设所有浏览器都正常。

应该：

```text
业务事件
   ↓
Push DTO
   ↓
Publisher
   ↓
每个 Session
   ├── 正常 → 发送
   ├── 超时 → 记录
   └── 断开 → 删除
```

至少要有：

```text
连接数
发送成功数
发送失败数
当前在线数
```

这些非常适合进入 VictoriaMetrics。

---

# 24. 第十四阶段：清理测试 Controller

当前 API 中存在：

```text
HelloController
TimeSeriesDataTestController
VmTestController
```

这些不应该长期存在正式 API。

建议：

```text
/controller
├── TrafficController
├── IntersectionController
├── AdviceController
└── DashboardController
```

测试接口移动：

```text
src/test
```

或者使用：

```text
application-test.yml
```

单独启用。

---

# 25. 第十五阶段：统一 API DTO

不要 Controller 直接返回：

```java
TrafficMetric
```

应该：

```text
Request DTO
     ↓
Application Command
     ↓
Domain
     ↓
Response DTO
```

例如：

```java
public record TrafficMetricResponse(
        String roadId,
        String direction,
        Integer trafficFlow,
        BigDecimal averageSpeed,
        BigDecimal co2Emission,
        Instant timestamp
) {}
```

这样数据库、InfluxDB、领域模型都不会直接暴露给前端。

---

# 26. 第十六阶段：统一异常

建议：

```text
common
└── exception
    ├── BusinessException
    ├── ErrorCode
    └── ErrorResponse
```

API：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

统一返回：

```json
{
  "code": "TRAFFIC_001",
  "message": "路口不存在",
  "traceId": "xxx",
  "data": null
}
```

不要把：

```java
exception.getMessage()
```

直接返回给前端。

---

# 27. 第十七阶段：配置文件重新组织

建议：

```text
application.yml
application-local.yml
application-test.yml
application-prod.yml
```

主配置：

```yaml
spring:
  application:
    name: green-traffic

  profiles:
    active: local
```

MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/green_traffic
    username: green_traffic
    password: ${GREEN_TRAFFIC_DB_PASSWORD}
```

Influx：

```yaml
traffic:
  influx:
    url: http://localhost:8086
    org: green-traffic
    bucket: traffic
    token: ${INFLUX_TOKEN}
```

RocketMQ：

```yaml
rocketmq:
  name-server: localhost:9876
```

不要把真实密码写进 Git。

---

# 28. 第十八阶段：数据库增加 Flyway

建议增加：

```text
db/migration
├── V1__init.sql
├── V2__add_intersection.sql
└── V3__add_advice.sql
```

以后数据库结构变化：

```text
修改 SQL
   ↓
提交 Git
   ↓
应用启动
   ↓
Flyway 自动升级
```

不要再手工执行生产数据库 SQL。

---

# 29. 第十九阶段：测试分层

最低要求：

```text
core
├── unit test
│   ├── CarbonCalculatorTest
│   └── OptimizationRuleTest
│
api
├── controller test
│
infrastructure
├── MyBatis integration test
├── RocketMQ integration test
└── InfluxDB integration test
```

核心业务测试最重要。

例如：

```java
@Test
void should_calculate_emission() {
    var result = calculator.calculate(input);

    assertThat(result.co2Emission())
            .isGreaterThan(BigDecimal.ZERO);
}
```

---

# 30. 第二十阶段：建立最小可观测性闭环

至少需要：

```text
/actuator/health
/actuator/prometheus
```

VictoriaMetrics 负责存：

```text
HTTP
JVM
MQ
DB
WebSocket
业务处理耗时
```

建议自定义：

```text
traffic_metric_received_total
traffic_metric_processed_total
traffic_metric_failed_total
traffic_metric_process_duration
traffic_websocket_connections
traffic_websocket_push_failed_total
traffic_mq_consume_failed_total
```

---

# 31. 推荐最终依赖关系

严格控制成：

```text
common
   ↑
model
   ↑
core
   ↑
infrastructure
   ↑
api

simulator
   ├── model
   └── core
```

但是：

```text
core 不能依赖 infrastructure
```

这是最重要的原则之一。

错误：

```text
core → MyBatis
core → RocketMQ
core → InfluxDB
core → RestTemplate
```

正确：

```text
core
 ↓
Port
 ↓
infrastructure adapter
 ↓
MyBatis / RocketMQ / InfluxDB
```

---

# 32. 推荐最终目录

```text
green-traffic
│
├── green-traffic-common
│   └── src/main/java
│       └── com.greentraffic.common
│           ├── exception
│           ├── response
│           └── trace
│
├── green-traffic-model
│   └── src/main/java
│       └── com.greentraffic.model
│           ├── command
│           ├── query
│           ├── dto
│           └── view
│
├── green-traffic-core
│   └── src/main/java
│       └── com.greentraffic.core
│           ├── application
│           ├── domain
│           │   ├── traffic
│           │   ├── carbon
│           │   └── optimization
│           └── port
│               ├── input
│               └── output
│
├── green-traffic-infrastructure
│   └── src/main/java
│       └── com.greentraffic.infrastructure
│           ├── config
│           ├── persistence
│           │   ├── mysql
│           │   └── influxdb
│           ├── messaging
│           │   ├── rocketmq
│           │   └── springevents
│           └── observability
│
├── green-traffic-api
│   └── src/main/java
│       └── com.greentraffic.api
│           ├── controller
│           ├── websocket
│           ├── advice
│           └── GreenTrafficApplication.java
│
├── green-traffic-simulator
│   └── src/main/java
│       └── com.greentraffic.simulator
│
└── green-traffic-dashboard
```

---

# 33. 小白执行顺序

不要同时改全部。

严格按照下面顺序执行。

## 第 1 步：备份

```bash
git checkout -b refactor/boot4-mybatis4-timeseries
git add .
git commit -m "chore: backup before architecture refactor"
```

## 第 2 步：只改 MyBatis

修改：

```text
pom.xml
```

只修改：

```xml
<mybatis-spring-boot.version>4.0.x</mybatis-spring-boot.version>
```

执行：

```bash
mvn clean verify -DskipTests
```

成功后提交：

```bash
git add .
git commit -m "build: upgrade mybatis starter for spring boot 4"
```

---

## 第 3 步：清理无用依赖

删除：

```text
spring-cloud.version
```

清理 Infrastructure POM。

执行：

```bash
mvn dependency:tree
```

检查有没有：

```text
spring-cloud
hibernate
jpa
```

如果业务已经完全使用 MyBatis，就不要继续保留 JPA。

---

## 第 4 步：先让 MySQL 链路稳定

实现：

```text
Controller
 ↓
UseCase
 ↓
RepositoryPort
 ↓
MyBatis Adapter
 ↓
MySQL
```

完成后测试：

```bash
mvn test
```

---

## 第 5 步：再接 InfluxDB

实现：

```text
TrafficMetric
 ↓
MetricWritePort
 ↓
InfluxMetricAdapter
 ↓
InfluxDB
```

先只完成：

```text
write
query
```

不要一开始就做缓存、批量、重试、异步。

先让功能正确。

---

## 第 6 步：再接 RocketMQ

实现：

```text
RocketMQ Consumer
 ↓
Message Converter
 ↓
WriteTrafficMetricCommand
 ↓
UseCase
```

完成后测试：

```text
发送 1 条
→ 消费 1 条
→ 写入 InfluxDB 1 条
```

---

## 第 7 步：再接 Spring Events

例如：

```text
TrafficMetricProcessedEvent
```

事件监听：

```text
Core
 ↓
Spring Event
 ↓
WebSocket Publisher
```

---

## 第 8 步：最后接 WebSocket

验证：

```text
浏览器连接
 ↓
SessionManager
 ↓
收到 TrafficMetricProcessedEvent
 ↓
JSON
 ↓
浏览器
```

---

## 第 9 步：接 VictoriaMetrics

不要把交通业务数据重新搬过去。

只接：

```text
Micrometer
 ↓
Prometheus endpoint
 ↓
vmagent / Prometheus
 ↓
VictoriaMetrics
```

---

# 34. 每完成一个阶段都做这个检查

```bash
mvn clean test
```

然后：

```bash
git status
```

确认没有误删。

再：

```bash
git diff
```

确认修改内容。

最后：

```bash
git commit -am "refactor: xxx"
```

不要一次提交 200 个文件。

---

# 35. 最终验收标准

项目达到下面状态，才算改造完成。

## 构建

```bash
mvn clean verify
```

必须成功。

## MySQL

可以：

```text
创建路口
查询路口
修改路口
删除路口
```

## InfluxDB

可以：

```text
写交通指标
批量写交通指标
按时间范围查询
按路口查询
```

## RocketMQ

可以：

```text
生产消息
消费消息
失败重试
重复消息幂等
```

## Spring Events

可以：

```text
业务处理完成
→ 发布事件
→ WebSocket 接收
```

## WebSocket

可以：

```text
浏览器连接
→ 收到实时数据
→ 断线自动清理
```

## VictoriaMetrics

可以看到：

```text
JVM
HTTP
MQ
WebSocket
业务处理耗时
```

---

# 36. 最终不要做的事情

不要：

```text
为了微服务而微服务
```

不要：

```text
Core 直接调用 RocketMQTemplate
```

不要：

```text
Core 直接操作 MyBatis Mapper
```

不要：

```text
Core 直接操作 InfluxDB Client
```

不要：

```text
把所有数据都放 VictoriaMetrics
```

不要：

```text
把所有数据都放 MySQL
```

不要：

```text
Spring Event 和 RocketMQ 做同一件事
```

不要：

```text
无限 BlockingQueue
```

不要：

```text
Controller 直接返回数据库 Entity
```

---

# 37. 推荐最终数据流

完整系统最终应该是：

```text
                    ┌───────────────┐
                    │   Sensor/SUMO │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   RocketMQ    │
                    └───────┬───────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Message Consumer     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Application Service  │
                 └──────────┬──────────┘
                            │
              ┌─────────────┼──────────────┐
              │             │              │
              ▼             ▼              ▼
          InfluxDB      Rule Engine    Spring Event
              │                            │
              │                            ▼
              │                       WebSocket
              │                            │
              │                            ▼
              │                        Dashboard
              │
              ▼
        Historical Query


          Application / JVM / MQ / HTTP
                       │
                       ▼
                   Micrometer
                       │
                       ▼
                Prometheus Endpoint
                       │
                       ▼
                vmagent / Prometheus
                       │
                       ▼
                VictoriaMetrics
```

---

# 38. 本次改造最重要的原则

记住下面 8 句话：

1. **Core 是业务，不是技术。**
2. **Port 是 Core 和外部世界之间的接口。**
3. **Adapter 才能接 MyBatis、RocketMQ、InfluxDB。**
4. **MySQL 保存业务实体，InfluxDB 保存业务时序。**
5. **VictoriaMetrics 保存技术监控指标。**
6. **Spring Event 负责进程内事件，RocketMQ 负责可靠异步消息。**
7. **WebSocket 是 API 层的输出适配器。**
8. **先保证数据正确，再做异步、批量、缓存和性能优化。**

---

## 官方兼容性参考

Spring Boot 4 要求 Java 17+，并基于 Spring Framework 7 / Jakarta EE 11。

MyBatis Spring Boot Starter 4.0.x 对应 Spring Boot 4.0、Java 17+。

VictoriaMetrics 可以通过 Prometheus remote_write 接收 Prometheus 指标。

RocketMQ Spring 官方项目支持 Spring Boot 集成，但升级到 Boot 4 时必须实际验证当前 starter 与 Spring Framework 7 的兼容性，不建议仅根据“Spring Boot 2.0+”这一宽泛说明判断兼容性。

---

## 改造完成后的版本基线

```text
Java                     17
Spring Boot              4.0.x
Spring Framework         7.x
Spring Web               Boot 4 starter
Spring Validation        Boot 4 starter
Spring WebSocket         Boot 4 starter
MyBatis                  MyBatis 3.5.x
MyBatis Spring           4.0.x
MyBatis Spring Boot      4.0.x
MySQL                    8.x
RocketMQ Spring          2.3.x，需按实际 Boot 4 兼容性验证
InfluxDB Java Client     7.x
VictoriaMetrics          最新稳定版
Maven                    3.9.x+
```

> 注意：不要为了追求“版本全部最新”而同时升级十几个组件。最稳妥的策略是一次只升级一个关键依赖，`mvn clean verify` 成功后再继续。
