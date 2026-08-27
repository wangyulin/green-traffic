# GreenTraffic 城市交通碳排放实时监测与优化系统 🌿

> 城市交通碳排放实时监测与智能优化系统

> 迁移说明：仓库正在把旧的技术导向端口（例如 `MetricWritePort` / `MetricQueryPort` / `SimulationMetricWritePort`）迁移为按业务能力命名的能力端口（`TrafficMetricStore` / `SimulationMetricStore`）。文档与迁移步骤见 `MIGRATION_PORTS.md` 与 `MIGRATION_MESSAGING.md`。

GreenTraffic 是一个面向城市交通管理场景的实时数据处理与碳排放分析系统。

系统通过**交通数据模拟器 / 传感器数据源**持续产生路口交通数据，经事件驱动机制进入核心业务层，完成：

- 交通数据采集与模拟
- 交通数据清洗与处理
- 碳排放量计算
- 交通拥堵分析
- 基于规则的优化建议生成
- 实时 WebSocket 推送
- 历史交通数据持久化
- 交通碳排放趋势分析
- 路口碳排放排行
- 监控大屏实时展示

项目当前采用 **Spring Boot + 事件驱动 + 模块化设计**，核心业务模块正在向**六边形架构（Hexagonal Architecture / Ports & Adapters）**演进。

---

## 1. 项目定位

GreenTraffic 的核心目标不是简单展示交通数据，而是建立一条完整的：

```text
交通数据产生
    ↓
实时事件
    ↓
业务处理
    ↓
碳排放计算
    ↓
优化规则分析
    ↓
数据持久化
    ↓
实时消息推送
    ↓
交通监控大屏
```

数据闭环。

系统将传统交通管理中的：

> “车多、拥堵、等待时间长”

转换为更加量化的：

> “车辆数 → 等待时间 → 碳排放 → 拥堵等级 → 优化建议”

从而形成面向交通管理的数据分析链路。

---

# 2. 核心能力

| 能力 | 说明 |
|---|---|
| 🚗 交通数据模拟 | 模拟城市路口传感器持续产生交通数据 |
| 📡 事件驱动 | 使用 Spring Application Event 解耦数据生产与业务处理 |
| 🧮 碳排放计算 | 根据车辆数、等待时间、车型比例、平均速度计算碳排放 |
| 🧠 规则引擎 | 根据交通状态生成优化建议 |
| 💾 数据持久化 | 保存交通实时数据及优化建议 |
| ⚡ 实时推送 | WebSocket 实时推送交通数据和告警 |
| 📊 数据分析 | 支持趋势、排行、小时级统计等查询 |
| 🖥️ 监控大屏 | HTML + ECharts 构建实时交通碳排放监控平台 |

项目设计文档中明确将系统定位为“实时感知、量化评估、智能建议、历史追溯”的交通碳排放监测系统。

---

# 3. 总体架构

## 3.1 当前系统总体架构

```text
┌─────────────────────────────────────────────────────────────────────┐
│                         Presentation / 展示层                       │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    Dashboard / ECharts                      │   │
│   │                                                             │   │
│   │  路口实时数据 │ 碳排放趋势 │ 排行 │ 告警 │ 优化建议 │ 热力图 │   │
│   └───────────────────────┬───────────────────────┬─────────────┘   │
│                           │ WebSocket             │ REST            │
└───────────────────────────┼───────────────────────┼─────────────────┘
                            │                       │
                            ▼                       ▼
                 ┌──────────────────┐    ┌──────────────────┐
                 │   Push Module    │    │    API Module    │
                 │                  │    │                  │
                 │ WebSocket        │    │ Controller       │
                 │ Session Manager  │    │ Query Service    │
                 │ Event Listener   │    │ Swagger / AOP    │
                 └────────┬─────────┘    └────────┬─────────┘
                          │                       │
                          │ ProcessedEvent        │ Query
                          │                       │
                          └──────────┬────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Core / 核心业务层                           │
│                                                                     │
│   ┌─────────────────┐     ┌──────────────────┐                     │
│   │ TrafficData     │     │ Carbon Emission  │                     │
│   │ Processor       │────▶│ Calculator        │                     │
│   └────────┬────────┘     └──────────────────┘                     │
│            │                                                        │
│            ▼                                                        │
│   ┌─────────────────────────────────────────┐                       │
│   │ Traffic Optimization Engine            │                       │
│   │                                         │                       │
│   │ WaitTime │ LowSpeed │ TruckRatio        │                       │
│   │ Vehicle  │ Emission │ Congestion        │                       │
│   └────────────────────┬────────────────────┘                       │
│                        │                                            │
│                        ▼                                            │
│                Repository / Port                                    │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         │ Persistence
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Infrastructure / 基础设施层                      │
│                                                                     │
│                    ┌──────────────────┐                             │
│                    │      MySQL       │                             │
│                    │                  │                             │
│                    │ intersection    │                             │
│                    │ traffic_data    │                             │
│                    │ optimization... │                             │
│                    └──────────────────┘                             │
└─────────────────────────────────────────────────────────────────────┘


                         ▲
                         │ GeneratedEvent
                         │
                ┌────────┴────────┐
                │    Simulator    │
                │                 │
                │ @Scheduled      │
                │                 │
                │ Uniform         │
                │ PeakHour        │
                │ Anomaly         │
                └─────────────────┘
```

当前仓库的模块划分已经包括 `green-traffic-core`、`green-traffic-simulator`、`green-traffic-push`、`green-traffic-api`、`green-traffic-infrastructure`、`green-traffic-common` 和 `green-traffic-dashboard` 等模块。

注：`green-traffic-model` 模块已移除；其领域模型已按本次重构迁移并归并到 `green-traffic-core` 或 `green-traffic-common`（文档中保留历史引用作为说明）。

---

# 4. 技术栈

## 4.1 后端

| 技术 | 用途 |
|---|---|
| Java 17 | 开发语言 |
| Spring Boot 3.2.x | 应用框架 |
| Spring Data JPA | 数据访问 |
| Spring Scheduling | 模拟器定时任务 |
| Spring Application Event | 模块间事件通信 |
| Spring WebSocket | 实时数据推送 |
| Spring Async | 异步事件处理 |
| Spring AOP | API 日志等横切能力 |
| Jackson | JSON 序列化 |
| Lombok | 简化 Java 样板代码 |
| springdoc-openapi | Swagger / OpenAPI |
| Maven | 项目构建 |

项目当前 README 给出的技术选型包括 Spring Boot 3.2.x、Java 17、Spring Data JPA、MySQL 8.x、H2、Spring WebSocket、Spring Scheduling、springdoc-openapi、Lombok、Jackson、ECharts 5.x 和 Maven 3.8+。

## 4.2 前端

```text
HTML
CSS
JavaScript
ECharts 5
WebSocket
REST API
```

Dashboard 采用静态资源方式集成在项目中，通过 REST API 获取历史/初始化数据，通过 WebSocket 接收实时数据。

## 4.3 数据库

```text
MySQL 8.x
H2（开发 / Demo）
```

主要数据表：

```text
intersection
      │
      │ 1:N
      ▼
traffic_data
      │
      │ 1:N
      ▼
optimization_advice
```

其中 `traffic_data` 保存实时交通数据及碳排放结果，`optimization_advice` 保存针对交通状态生成的优化建议。

---

# 5. Maven 多模块结构

```text
green-traffic
│
├── green-traffic-api
│   └── 应用入口 / Controller / AOP / Configuration
│
├── green-traffic-common
│   └── 公共响应 / 异常 / 工具 / 通用定义
│
├── green-traffic-core
│   └── 核心业务 / Domain / Port / Service / Rule / Repository Port
│
├── green-traffic-dashboard
│   └── 前端静态页面 / ECharts / WebSocket Client
│
├── green-traffic-doc
│   └── 项目设计及文档
│
├── green-traffic-infrastructure
│   └── 数据库 / Repository Adapter / 基础设施实现
│
<!-- green-traffic-model 已移除：模型已迁移到 core 或 common -->
│
├── green-traffic-push
│   └── WebSocket / 实时消息推送
│
├── green-traffic-simulator
│   └── 交通传感器模拟 / 数据生成
│
├── sumo-work
│   └── SUMO 仿真相关工程
│
└── pom.xml
```

项目当前模块依赖关系整体上已经形成：

```text
                       common
                         │
                         ▼
                       model
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   simulator           core             push
                           │
                           │
                           ▼
                         api
                           │
                           ▼
                       dashboard
```

仓库原始设计中也明确采用这种模块化依赖关系，并将 API 模块作为最终应用启动入口。

---

# 6. 六边形架构设计

## 6.1 为什么采用六边形架构

传统 Spring Boot 项目通常是：

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

这种方式简单，但是随着系统增加：

- WebSocket
- 模拟器
- 消息队列
- 第三方传感器
- SUMO 仿真
- 数据库
- AI / 算法模型

业务代码容易逐渐依赖具体技术实现。

例如：

```java
TrafficDataProcessor
        ↓
JpaRepository
        ↓
MySQL
```

最终导致：

```text
业务逻辑
    ↓
Spring
    ↓
JPA
    ↓
MySQL
```

业务核心被基础设施绑死。

GreenTraffic 更适合采用：

```text
                 ┌───────────────┐
                 │   REST API    │
                 └───────┬───────┘
                         │
                     Input Adapter
                         │
                         ▼
              ┌──────────────────────┐
              │                      │
              │     Application      │
              │       Core           │
              │                      │
              │ Carbon Calculator    │
              │ Optimization Engine  │
              │ Traffic Processing   │
              │ Domain Rules         │
              │                      │
              └──────────────────────┘
                         ▲
                         │
                    Output Port
                         │
                 ┌───────┴────────┐
                 │                │
                 ▼                ▼
              MySQL          WebSocket
             Adapter           Adapter
```

核心业务不关心：

> “数据来自哪里？”

也不关心：

> “最终保存到 MySQL、Kafka 还是其他系统？”

它只依赖抽象的 Port。

---

# 7. GreenTraffic 六边形架构映射

可以把当前项目映射成下面的结构：

```text
                        ┌────────────────────┐
                        │     REST API       │
                        │ green-traffic-api  │
                        └─────────┬──────────┘
                                  │
                                  │ Input Adapter
                                  ▼
                    ┌──────────────────────────┐
                    │                          │
                    │       Input Ports        │
                    │                          │
                    │ TrafficQueryUseCase      │
                    │ AdviceQueryUseCase       │
                    │ IntersectionUseCase      │
                    │                          │
                    └────────────┬─────────────┘
                                 │
                                 ▼
              ┌────────────────────────────────────────┐
              │                                        │
              │              DOMAIN / CORE              │
              │                                        │
              │  TrafficDataProcessor                  │
              │          │                             │
              │          ├── CarbonEmissionCalculator │
              │          │                             │
              │          └── OptimizationEngine        │
              │                  │                     │
              │                  ├── WaitTimeRule      │
              │                  ├── LowSpeedRule      │
              │                  ├── VehicleRule       │
              │                  ├── EmissionRule      │
              │                  └── CongestionRule    │
              │                                        │
              └──────────────┬─────────────────────────┘
                             │
                    Output Ports
                             │
              ┌──────────────┼─────────────────┐
              │              │                 │
              ▼              ▼                 ▼
        Repository Port   Event Port       Push Port
              │              │                 │
              ▼              ▼                 ▼
        MySQL Adapter    Event Adapter    WebSocket
        Infrastructure                     Adapter
```

这里最重要的设计原则是：

> **依赖关系必须指向业务核心，而不是反过来。**

即：

```text
Adapter ────────▶ Port ────────▶ Core
```

而不是：

```text
Core ────────▶ MySQL
Core ────────▶ WebSocket
Core ────────▶ Controller
```

---

# 8. Port 与 Adapter 的职责

## 8.1 Input Port

Input Port 表示：

> “外部世界可以让系统做什么？”

例如：

```text
查询最新交通数据
查询历史趋势
查询碳排放排行
查询优化建议
处理交通数据
```

Controller 不应该直接操作 Repository，而应该调用 Input Port。

```text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Input Port
     │
     ▼
Application / Domain
```

---

# 9. Output Port

Output Port 表示：

> “核心业务需要外部世界提供什么能力？”

例如：

```text
TrafficDataRepositoryPort
OptimizationAdviceRepositoryPort
EventPublisherPort
TrafficMessagePublisherPort
```

核心业务只知道：

```java
trafficDataRepositoryPort.save(data);
```

而不知道：

```text
JPA
Hibernate
MySQL
Redis
Kafka
```

具体实现放在 Adapter。

---

# 10. Simulator 也是一个 Adapter

这是 GreenTraffic 六边形架构里非常重要的一点。

现在 Simulator 是系统的数据输入源：

```text
Simulator
    │
    ▼
TrafficDataGeneratedEvent
    │
    ▼
Core
```

从架构角度来看：

```text
             ┌────────────────────┐
             │      Simulator     │
             │                    │
             │ @Scheduled         │
             │ DataGenerator      │
             └─────────┬──────────┘
                       │
                  Input Adapter
                       │
                       ▼
             ┌────────────────────┐
             │      Input Port    │
             │                    │
             │ TrafficDataInput   │
             └─────────┬──────────┘
                       │
                       ▼
                  Core Domain
```

未来如果接入真实传感器：

```text
                 Core
                  ▲
                  │
        ┌─────────┼─────────┐
        │         │         │
        │         │         │
    Simulator   Kafka     MQTT
      Adapter   Adapter   Adapter
```

核心业务完全不需要修改。

这就是六边形架构最大的价值之一。

---

# 11. 数据生成策略

当前 Simulator 设计采用 `DataGenerator` 抽象，并提供不同数据生成策略，包括：

```text
DataGenerator
      │
      ├── UniformDataGenerator
      │
      ├── PeakHourDataGenerator
      │
      └── AnomalyDataGenerator
```

同时 Simulator 根据当前时间段选择生成策略。

项目当前配置默认：

```yaml
simulator:
  interval-ms: 5000
  intersection-count: 6
  peak-hour-enabled: true
  anomaly-enabled: true
  anomaly-probability: 0.05
```

即默认每 5 秒产生一次模拟数据，并支持高峰时段及异常数据模拟。

---

# 12. 三类仿真任务

## 12.1 普通流量任务

```text
UniformDataGenerator
```

用于模拟正常交通状态。

特点：

```text
车辆数        正常
平均等待时间   正常
平均速度       正常
卡车比例       正常
碳排放         正常
```

适用于：

- 基础数据链路测试
- WebSocket 推送测试
- 数据库写入测试
- Dashboard 展示测试

---

# 13. 高峰交通任务

```text
PeakHourDataGenerator
```

根据当前时间段模拟城市交通高峰。

项目设计中定义：

| 时间段 | 车辆数 | 等待时间 | 速度 |
|---|---:|---:|---:|
| 早高峰 07:00-09:00 | ×1.5 | +20s | -15km/h |
| 午间 11:00-13:00 | ×1.2 | +5s | -5km/h |
| 晚高峰 17:00-19:00 | ×1.8 | +30s | -20km/h |
| 深夜 23:00-05:00 | ×0.3 | -20s | +20km/h |
| 平峰 | ×1.0 | 正常 | 正常 |



高峰任务主要用于验证：

```text
交通流量增加
      ↓
等待时间增加
      ↓
平均速度下降
      ↓
碳排放增加
      ↓
优化规则触发
      ↓
产生优化建议
      ↓
HIGH / MEDIUM / LOW
```

---

# 14. 异常交通任务

```text
AnomalyDataGenerator
```

用于模拟异常交通场景。

例如：

```text
车辆数异常增加
平均等待时间异常
平均速度异常降低
卡车比例异常
碳排放异常升高
```

最终用于验证规则：

```text
HighVehicleCountRule
LowSpeedRule
LongWaitTimeRule
HighTruckRatioRule
HighEmissionRule
PersistentCongestionRule
```

这些规则均属于 Core 中的业务规则，而不是 Simulator 的职责。

---

# 15. 一条完整数据链路

这是整个项目最核心的一条链路：

```text
┌──────────────────┐
│ @Scheduled       │
│ Simulator        │
└────────┬─────────┘
         │
         │ ① 生成交通数据
         ▼
┌────────────────────────┐
│ DataGenerator          │
│                        │
│ Uniform / Peak /       │
│ Anomaly                │
└────────┬───────────────┘
         │
         │ TrafficDataDTO
         ▼
┌────────────────────────┐
│ TrafficDataGenerated   │
│ Event                  │
└────────┬───────────────┘
         │
         │ Spring Event
         ▼
┌────────────────────────┐
│ TrafficDataProcessor   │
│                        │
│ @EventListener         │
│ @Async                 │
│ @Transactional         │
└────────┬───────────────┘
         │
         ├──────────────────────────────┐
         │                              │
         ▼                              ▼
┌──────────────────┐          ┌────────────────────┐
│ CarbonEmission   │          │ OptimizationEngine │
│ Calculator       │          │                    │
└────────┬─────────┘          └─────────┬──────────┘
         │                              │
         │ emission                    │ advices
         └──────────────┬───────────────┘
                        ▼
              ┌──────────────────┐
              │ TrafficData      │
              │ Entity           │
              └────────┬─────────┘
                       │
                       │ save()
                       ▼
              ┌──────────────────┐
              │ MySQL            │
              │ traffic_data     │
              └──────────────────┘
                       │
                       │ advice
                       ▼
              ┌──────────────────────┐
              │ optimization_advice  │
              └──────────────────────┘
```

项目当前 `TrafficDataProcessor` 的设计就是：

1. 接收 `TrafficDataGeneratedEvent`
2. 计算碳排放
3. 执行优化规则
4. 组装 `TrafficData`
5. 保存交通数据
6. 保存优化建议
7. 发布 `TrafficDataProcessedEvent`



---

# 16. 仿真任务 → 数据库完整链路

如果从“几个仿真任务”分别来看，可以理解成三条输入链路汇聚到同一个 Core。

## 16.1 普通流量

```text
UniformDataGenerator
        │
        ▼
TrafficDataDTO
        │
        ▼
GeneratedEvent
        │
        ▼
TrafficDataProcessor
        │
        ├── CarbonEmissionCalculator
        │
        ├── OptimizationEngine
        │
        ▼
TrafficData
        │
        ▼
MySQL
```

---

## 16.2 高峰流量

```text
PeakHourDataGenerator
        │
        │
        │ 车辆数 ↑
        │ 等待时间 ↑
        │ 速度 ↓
        ▼
TrafficDataDTO
        │
        ▼
GeneratedEvent
        │
        ▼
TrafficDataProcessor
        │
        ├── CarbonEmissionCalculator
        │          │
        │          ▼
        │      Emission ↑
        │
        ├── OptimizationEngine
        │          │
        │          ├── LongWaitTimeRule
        │          ├── LowSpeedRule
        │          ├── HighVehicleCountRule
        │          └── HighEmissionRule
        │
        ▼
TrafficData + OptimizationAdvice
        │
        ▼
MySQL
```

---

## 16.3 异常流量

```text
AnomalyDataGenerator
        │
        ▼
异常 TrafficDataDTO
        │
        ▼
GeneratedEvent
        │
        ▼
TrafficDataProcessor
        │
        ├── CarbonEmissionCalculator
        │
        ├── OptimizationEngine
        │       │
        │       ├── HighEmissionRule
        │       ├── LowSpeedRule
        │       ├── LongWaitTimeRule
        │       └── PersistentCongestionRule
        │
        ▼
OptimizationAdvice
        │
        ├── LOW
        ├── MEDIUM
        └── HIGH
        │
        ▼
MySQL
```

---

# 17. 更完整的实时数据链路

真正运行时，不只是写数据库，还存在一条实时推送链路。

因此可以把整个系统理解成：

```text
                       ┌──────────────────┐
                       │ Traffic Simulator│
                       └────────┬─────────┘
                                │
                                │ GeneratedEvent
                                ▼
                       ┌──────────────────┐
                       │ TrafficData      │
                       │ Processor        │
                       └────────┬─────────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               ▼                ▼                ▼
        Carbon Calculator   Rule Engine      Repository
               │                │                │
               │                │                ▼
               │                │             MySQL
               │                │
               └────────┬───────┘
                        │
                        ▼
               ProcessedEvent
                        │
                        ▼
               ┌──────────────────┐
               │ TrafficPush      │
               │ Listener         │
               └────────┬─────────┘
                        │
                        ▼
               ┌──────────────────┐
               │ WebSocket Handler│
               └────────┬─────────┘
                        │
                        │ Broadcast
                        ▼
               ┌──────────────────┐
               │ Dashboard        │
               │                  │
               │ 实时数据          │
               │ 优化建议          │
               │ 告警              │
               └──────────────────┘
```

Push 模块监听 `TrafficDataProcessedEvent`，然后分别推送：

```text
TRAFFIC_DATA
OPTIMIZATION_ADVICE
ALERT
```

同时 WebSocket 客户端还实现了心跳和自动重连机制。

---

# 18. 为什么使用 Spring Event

这里的事件机制实际上承担了一个非常重要的架构职责：

```text
Simulator
    │
    │ 不直接调用 Core Service
    ▼
GeneratedEvent
    │
    ▼
Core
```

Core 完成之后：

```text
Core
 │
 │ 不直接调用 WebSocket
 ▼
ProcessedEvent
 │
 ├───────────────▶ Push
 │
 └───────────────▶ Future Adapter
```

这样可以避免：

```text
Simulator
   ↓
TrafficDataProcessor
   ↓
WebSocket
   ↓
Dashboard
```

形成强耦合调用链。

而变成：

```text
Producer
   ↓
Event
   ↓
Consumer
```

因此后续可以很容易增加：

```text
ProcessedEvent
      │
      ├── WebSocket
      ├── Kafka
      ├── Elasticsearch
      ├── Data Lake
      ├── AI Analysis
      └── Monitoring
```

而不需要修改核心业务代码。

---

# 19. 碳排放计算模型

当前系统使用车辆数量、等待时间、卡车比例和平均速度进行碳排放计算。

基本模型：

```text
基础排放量
    =
车辆数
×
怠速排放率
×
等待时间
```

然后加入：

```text
车型权重
+
速度修正
```

最终得到：

```text
Carbon Emission
```

当前实现中，小汽车基础怠速排放率为 `0.0307 kg/min`，并根据卡车比例及速度进行修正。

这意味着系统的数据模型不是单纯的：

```text
vehicle_count
```

而是：

```text
vehicle_count
       │
       ├── avg_wait_time
       │
       ├── avg_speed
       │
       ├── truck_ratio
       │
       ▼
carbon_emission
```

---

# 20. 规则引擎

规则引擎采用策略接口：

```java
OptimizationRule
```

每个规则负责：

```text
1. 优先级
2. 是否触发
3. 生成建议
4. 规则编号
5. 规则描述
```

当前设计包含：

```text
OptimizationRule
        │
        ├── LongWaitTimeRule
        ├── LowSpeedRule
        ├── HighTruckRatioRule
        ├── HighVehicleCountRule
        ├── HighEmissionRule
        └── PersistentCongestionRule
```

例如：

```text
平均等待时间 > 60 秒
        │
        ▼
LongWaitTimeRule
        │
        ▼
信号灯配时建议
        │
        ▼
HIGH
```



这种设计天然符合：

> **Open/Closed Principle**

新增规则时只需要增加新的 `OptimizationRule` 实现，而不需要修改核心规则引擎。

---

# 21. 数据库设计

## 21.1 intersection

保存路口基础信息：

```text
intersection
├── id
├── intersection_id
├── name
├── district
├── lanes
├── has_signal_light
├── location_lat
├── location_lng
├── description
└── create_time
```

---

## 21.2 traffic_data

保存实时交通数据：

```text
traffic_data
├── id
├── intersection_id
├── vehicle_count
├── avg_wait_time
├── avg_speed
├── truck_ratio
├── carbon_emission
└── create_time
```

并针对：

```text
intersection_id + create_time
```

建立联合索引，用于路口时间范围查询。

---

## 21.3 optimization_advice

保存业务优化建议：

```text
optimization_advice
├── id
├── traffic_data_id
├── intersection_id
├── level
├── title
├── content
├── status
└── create_time
```

关系：

```text
traffic_data
     │
     │ 1:N
     ▼
optimization_advice
```



---

# 22. REST API

API 模块主要提供：

```text
/api/traffic
/api/analysis
/api/advice
/api/intersection
/api/simulator
```

例如：

```http
GET /api/traffic/latest/{intersectionId}
```

获取某个路口最新交通数据。

```http
GET /api/traffic/latest/all
```

获取所有路口最新数据。

```http
GET /api/traffic/trend
```

获取路口碳排放趋势。

同时系统使用 OpenAPI / Swagger 描述接口。

---

# 23. WebSocket 实时消息

WebSocket 消息类型：

```text
CONNECTED
TRAFFIC_DATA
OPTIMIZATION_ADVICE
ALERT
HEARTBEAT
```

其中：

```text
TRAFFIC_DATA
```

用于推送实时交通数据。

```text
OPTIMIZATION_ADVICE
```

用于推送优化建议。

```text
ALERT
```

用于推送高优先级告警。



---

# 24. API 与 WebSocket 的职责边界

项目同时使用 REST 和 WebSocket，两者职责不同：

```text
                    Dashboard
                       │
            ┌──────────┴──────────┐
            │                     │
          REST                 WebSocket
            │                     │
            ▼                     ▼
       查询历史数据             实时数据
       初始化页面             实时交通数据
       趋势分析               优化建议
       排行统计               高优先级告警
```

因此：

> REST = Query / Pull

> WebSocket = Event / Push

这种职责划分比较合理。

---

# 25. 项目当前架构的核心特点

GreenTraffic 当前最值得学习的不是某一个技术，而是以下几个架构思想：

### ① 模块化

```text
API
Core
Simulator
Push
Infrastructure
Dashboard
```

不同能力进行模块隔离。

### ② 事件驱动

```text
GeneratedEvent
ProcessedEvent
```

生产者和消费者通过事件解耦。

### ③ 六边形架构

```text
Core
 ▲
 │
Port
 ▲
 │
Adapter
```

业务核心与数据库、WebSocket、Simulator 等技术实现隔离。

### ④ 策略模式

```text
DataGenerator
OptimizationRule
```

让不同数据生成策略和业务规则可以独立扩展。

### ⑤ CQRS 倾向

虽然目前并非严格 CQRS，但系统已经存在明显的：

```text
Command / Processing
        +
Query
```

分离趋势：

```text
实时事件
   ↓
数据处理
   ↓
数据库

REST API
   ↓
Query Service
   ↓
数据库
```

### ⑥ 实时计算 + 历史查询

系统同时满足：

```text
实时场景
    ↓
WebSocket

历史分析
    ↓
REST + JPA
```

---

# 26. 推荐的标准六边形目录结构

如果继续进行架构重构，建议 Core 最终收敛成下面这种结构：

```text
green-traffic-core
│
└── src/main/java/com/greentraffic/core
    │
    ├── domain
    │   ├── traffic
    │   │   ├── TrafficData.java
    │   │   ├── TrafficDataService.java
    │   │   └── TrafficDataPolicy.java
    │   │
    │   ├── emission
    │   │   ├── CarbonEmissionCalculator.java
    │   │   └── EmissionFactor.java
    │   │
    │   └── optimization
    │       ├── OptimizationRule.java
    │       ├── OptimizationEngine.java
    │       └── rules/
    │
    ├── application
    │   ├── service
    │   │   ├── TrafficDataApplicationService.java
    │   │   └── TrafficQueryApplicationService.java
    │   │
    │   └── dto
    │
    ├── port
    │   ├── input
    │   │   ├── ProcessTrafficDataUseCase.java
    │   │   ├── QueryTrafficDataUseCase.java
    │   │   └── QueryAdviceUseCase.java
    │   │
    │   └── output
    │       ├── TrafficDataRepository.java
    │       ├── AdviceRepository.java
    │       └── EventPublisher.java
    │
    └── event
        ├── TrafficDataGeneratedEvent.java
        └── TrafficDataProcessedEvent.java
```

然后：

```text
green-traffic-infrastructure
│
├── persistence
│   ├── JpaTrafficDataRepository.java
│   └── JpaAdviceRepository.java
│
└── messaging
    └── SpringEventPublisher.java
```

```text
green-traffic-simulator
│
└── adapter
    └── TrafficSensorSimulatorAdapter.java
```

```text
green-traffic-push
│
└── adapter
    └── WebSocketTrafficPushAdapter.java
```

```text
green-traffic-api
│
└── adapter
    └── rest
        ├── TrafficController.java
        ├── AdviceController.java
        └── AnalysisController.java
```

最终形成：

```text
                 ┌─────────────────────┐
                 │      REST API       │
                 │    Input Adapter    │
                 └──────────┬──────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │  Input Port  │
                    └──────┬───────┘
                           │
                           ▼
        ┌────────────────────────────────────┐
        │                                    │
        │               CORE                 │
        │                                    │
        │ Domain + Application Service       │
        │                                    │
        └────────────────┬───────────────────┘
                         │
                    Output Port
                         │
              ┌──────────┼───────────┐
              │          │           │
              ▼          ▼           ▼
           MySQL     Spring Event  WebSocket
           Adapter     Adapter      Adapter
```

这会比单纯按照：

```text
controller
service
repository
```

划分更加符合这个项目的业务特点。

---

# 27. 从当前版本到标准六边形架构的演进路线

建议不要一次性大规模重构，而是分阶段进行。

## Phase 1：明确 Core 边界

```text
green-traffic-core
```

只保留：

```text
业务规则
业务服务
领域模型
Port
Domain Event
```

---

## Phase 2：数据库完全 Adapter 化

当前：

```text
Core
 ↓
JpaRepository
 ↓
MySQL
```

逐渐调整为：

```text
Core
 ↓
TrafficDataRepositoryPort
 ↓
Infrastructure Adapter
 ↓
JPA
 ↓
MySQL
```

---

## Phase 3：消息完全 Port 化

当前：

```text
ApplicationEventPublisher
```

逐渐抽象：

```text
Core
 ↓
EventPublisherPort
 ↓
SpringEventAdapter
```

这样以后替换成 Kafka：

```text
Core
 ↓
EventPublisherPort
 ↓
KafkaEventAdapter
 ↓
Kafka
```

Core 无需修改。

---

## Phase 4：WebSocket 完全 Adapter 化

最终：

```text
Core
 ↓
TrafficMessagePublisher
 ↓
WebSocketAdapter
 ↓
Dashboard
```

未来可以变成：

```text
                    TrafficMessagePort
                           │
              ┌────────────┼─────────────┐
              ▼            ▼             ▼
         WebSocket       Kafka        MQTT
          Adapter        Adapter      Adapter
```

---

# 28. 推荐的最终架构

如果这个项目后续继续发展，我建议最终目标定为：

```text
                             ┌───────────────┐
                             │   Dashboard   │
                             └───────▲───────┘
                                     │
                                WebSocket
                                     │
                          ┌──────────┴──────────┐
                          │ WebSocket Adapter   │
                          └──────────▲──────────┘
                                     │
                                     │
                   ┌─────────────────┴──────────────────┐
                   │                                    │
                   │          GREEN TRAFFIC CORE        │
                   │                                    │
                   │  ┌──────────────────────────────┐  │
                   │  │       Application Layer      │  │
                   │  │                              │  │
                   │  │ Traffic Processing           │  │
                   │  │ Query                        │  │
                   │  │ Analysis                     │  │
                   │  └──────────────┬───────────────┘  │
                   │                 │                  │
                   │  ┌──────────────▼───────────────┐  │
                   │  │          Domain              │  │
                   │  │                              │  │
                   │  │ Carbon Calculation           │  │
                   │  │ Optimization Rules            │  │
                   │  │ Traffic Domain                │  │
                   │  └──────────────┬───────────────┘  │
                   │                 │                  │
                   │             Output Port            │
                   └─────────────────┼──────────────────┘
                                     │
               ┌─────────────────────┼──────────────────────┐
               │                     │                      │
               ▼                     ▼                      ▼
        Repository Adapter     Event Adapter        Message Adapter
               │                     │                      │
               ▼                     ▼                      ▼
             MySQL             Kafka/EventBus          WebSocket


          Input Side
               ▲
               │
      ┌────────┴────────┐
      │                 │
 Simulator Adapter    REST Adapter
      │                 │
      │                 │
      ▼                 ▼
 Traffic Sensor       API Client
```

这个架构的核心原则只有一句话：

> **业务核心稳定，外部技术可替换。**

---

# 29. 数据链路总结

GreenTraffic 最核心的一条业务链路可以浓缩成：

```text
                ┌─────────────────┐
                │ Traffic Sensor  │
                │ / Simulator     │
                └────────┬────────┘
                         │
                         │ TrafficDataDTO
                         ▼
                ┌─────────────────┐
                │ GeneratedEvent  │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ TrafficData     │
                │ Processor       │
                └────────┬────────┘
                         │
             ┌───────────┴───────────┐
             │                       │
             ▼                       ▼
    ┌─────────────────┐     ┌──────────────────┐
    │ Carbon Emission │     │ Optimization     │
    │ Calculator      │     │ Engine           │
    └────────┬────────┘     └────────┬─────────┘
             │                       │
             └───────────┬───────────┘
                         ▼
                ┌─────────────────┐
                │ TrafficData     │
                │ + Advice        │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │    MySQL        │
                └─────────────────┘
                         │
                         │ ProcessedEvent
                         ▼
                ┌─────────────────┐
                │ Push Listener   │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ WebSocket       │
                └────────┬────────┘
                         │
                         ▼
                ┌─────────────────┐
                │ Dashboard       │
                └─────────────────┘
```

---

# 30. 项目运行

## 环境要求

```text
JDK 17+
Maven 3.8+
MySQL 8.x
```

开发 / Demo 环境可以使用 H2。

## 构建

```bash
mvn clean package
```

## 启动

```bash
mvn spring-boot:run
```

或者运行：

```text
green-traffic-api
```

中的：

```text
GreenTrafficApplication
```

项目启动类启用了：

```java
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan
```

因此系统启动后会同时具备：

```text
定时模拟
+
异步事件处理
+
AOP
+
配置绑定
```



---

# 31. 项目适合继续演进的方向

当前项目已经具备比较好的架构演进基础，后续可以继续扩展：

```text
                     GreenTraffic
                          │
        ┌─────────────────┼──────────────────┐
        │                 │                  │
        ▼                 ▼                  ▼
    实时交通           碳排放分析          智能优化
        │                 │                  │
        ▼                 ▼                  ▼
     Kafka             OLAP/ES             AI/ML
        │                 │                  │
        └─────────────────┼──────────────────┘
                          │
                          ▼
                    城市交通大脑
```

进一步可以引入：

- Kafka / Pulsar
- Redis
- Elasticsearch
- TimescaleDB / ClickHouse
- SUMO
- AI 交通预测
- 强化学习信号灯优化
- 实时流处理
- Prometheus + Grafana
- OpenTelemetry
- Docker / Kubernetes

最终从：

> **交通碳排放监控 Demo**

演进为：

> **城市交通实时数据 + 碳排放分析 + 智能优化平台**

---

# 32. 架构设计原则

GreenTraffic 遵循以下核心原则：

```text
┌───────────────────────────────────────┐
│         GreenTraffic Architecture     │
├───────────────────────────────────────┤
│                                       │
│  ① Core First                         │
│     业务核心优先于技术实现             │
│                                       │
│  ② Dependency Inversion               │
│     业务依赖抽象，而不是依赖数据库      │
│                                       │
│  ③ Ports & Adapters                   │
│     外部系统通过 Adapter 接入          │
│                                       │
│  ④ Event Driven                       │
│     通过事件降低模块耦合               │
│                                       │
│  ⑤ Strategy                           │
│     数据生成和优化规则可插拔           │
│                                       │
│  ⑥ Open / Closed                      │
│     新增规则尽量不修改核心代码         │
│                                       │
│  ⑦ Real-time + History                │
│     实时推送与历史分析并存             │
│                                       │
└───────────────────────────────────────┘
```

---

## License

本项目用于城市交通碳排放监测、软件架构实践及相关技术研究。

---

## Repository

项目地址：

[wangyulin/green-traffic](https://github.com/wangyulin/green-traffic?utm_source=chatgpt.com)