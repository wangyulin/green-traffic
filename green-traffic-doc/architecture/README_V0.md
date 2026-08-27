# GreenTraffic 城市交通碳排放实时监测与优化系统

## 项目完整设计文档

---

## 一、项目简介

### 1.1 项目背景

随着城市化进程加速，交通拥堵和机动车碳排放已成为城市环境治理的核心难题。据环保部门统计，城市交通碳排放占城市总碳排放的 **20%~30%**，而路口怠速等待是造成无效排放的主要原因之一。

传统交通管理方式存在以下痛点：

| **响应被动** | 往往拥堵形成后才采取措施，缺乏预警机制 |
| **信息孤岛** | 路口数据分散，缺乏统一监控平台 |

### 1.2 项目定位

**GreenTraffic** 是一个面向城市交通管理部门的**实时碳排放监测与智能优化建议系统**。通过模拟部署在城市各路口的传感器，实时采集车流数据，计算碳排放量，结合规则引擎生成交通优化建议，并通过 WebSocket 推送到监控大屏，帮助管理者**实时感知、量化评估、主动优化**。

### 1.3 核心价值

```
实时感知 ──→ 每5秒采集一次路口数据，秒级推送至大屏
量化评估 ──→ 将抽象的"拥堵"转化为具体的"碳排放量(kg)"
智能建议 ──→ 基于规则引擎自动生成可执行的优化方案
历史追溯 ──→ 完整保留历史数据，支持趋势分析和决策复盘
```

### 1.4 技术亮点

| 技术点 | 应用场景 | 教学价值 |
|--------|---------|---------|
| **Spring Event** | 模块间解耦通信 | 理解观察者模式、事件驱动架构 |
| **WebSocket** | 实时数据推送 | 对比传统轮询的优劣 |
| **规则引擎** | 优化建议生成 | 开闭原则、策略模式实践 |
| **AOP** | 接口日志记录 | 横切关注点分离 |
| **全局异常** | 统一错误处理 | 理解 Spring 异常传播机制 |
| **JPA 聚合查询** | 统计分析 | 掌握复杂查询编写 |

---

## 二、系统架构设计

### 2.1 总体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端展示层                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Dashboard 监控大屏（HTML + ECharts）         │    │
│  │  路口卡片 │ 告警列表 │ 趋势折线图 │ 排行柱状图 │ 热力图        │    │
│  └──────────────┬──────────────────────┬───────────────────┘    │
│                 │ WebSocket            │ REST API               │
├─────────────────┼──────────────────────┼───────────────────────┤
│                 ↓                      ↓                        │
│  ┌──────────────────────┐  ┌──────────────────────────────┐    │
│  │   Push 推送模块       │  │       API 接口模块             │    │
│  │  WebSocket 连接管理   │  │  Controller / AOP / Swagger  │    │
│  │  消息广播 / 心跳检测   │  │  参数校验 / 统一响应            │    │
│  └─────────┬────────────┘  └──────────────┬───────────────┘    │
│            │                               │                    │
│            │ 监听 ProcessedEvent           │ 调用查询服务        │
├────────────┼───────────────────────────────┼───────────────────┤
│            ↓                               ↓                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Core 核心业务模块                      │    │
│  │  ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐  │    │
│  │  │ 碳排放计算引擎 │ │ 规则引擎       │ │  数据持久化      │  │    │
│  │  │ Calculator   │ │ Optimizer    │ │  Repository     │  │    │
│  │  └──────────────┘ └──────────────┘ └─────────────────┘  │    │
│  └─────────────────────────┬───────────────────────────────┘    │
│                            │ 监听 GeneratedEvent                 │
├────────────────────────────┼───────────────────────────────────┤
│                            ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                 Simulator 传感器模拟模块                  │    │
│  │  @Scheduled 定时任务  │ 随机数据生成策略  │ 高峰时段模型      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│                          基础设施层                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │
│  │    MySQL    │  │    H2(备)   │  │  GreenTrafficCommon     │   │
│  │  数据存储    │  │  演示模式    │   │  统一响应/异常/工具       │   │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 分类 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| 基础框架 | Spring Boot | 3.2.x | 主流、生态完善 |
| JDK | Java | 17 | LTS 版本，Spring Boot 3 最低要求 |
| ORM | Spring Data JPA | - | 简化数据访问，教学友好 |
| 数据库 | MySQL | 8.x | 主流关系型数据库 |
| 备选数据库 | H2 | 2.x | 演示无需安装，零配置 |
| 实时推送 | Spring WebSocket | - | 原生支持，无需额外依赖 |
| 定时任务 | Spring Scheduling | - | 内置支持，简单易用 |
| 接口文档 | springdoc-openapi | 2.x | 自动生成 Swagger UI |
| 简化代码 | Lombok | - | 减少样板代码 |
| 序列化 | Jackson | - | Spring 默认集成 |
| 前端图表 | ECharts | 5.x | 国产开源，功能强大 |
| 构建工具 | Maven | 3.8+ | 主流构建工具 |

### 2.3 模块依赖关系

```
                    ┌──────────────────┐
                    │  green-traffic-   │
                    │     common        │
                    └────────┬─────────┘
                             │ 被所有模块依赖
                    ┌────────▼─────────┐
                    │  green-traffic-   │
                    │     model         │
                    └────────┬─────────┘
                             │
        ┌────────────┬───────┼──────── ───┬────────────┐
        │            │       │            │            │
┌───────▼──────┐ ┌───▼────┐ ┌▼─────────┐ ┌▼─────────┐
│  simulator   │ │  core  │ │   push   │ │   api    │
│              │ │        │ │          │ │          │
└──────────────┘ └───┬────┘ └──────────┘ └────┬─────┘
                     │                         │
                     └──────────┬──────────────┘
                                │ 最终由 api 模块聚合
                                │ 作为应用启动入口
                      ┌─────────▼─────────┐
                      │   dashboard       │
                      │  (静态资源模块)     │
                      └───────────────────┘
```

### 2.4 数据流全景图

```
┌────────────────────────────────────────────────────────────────────┐
│                          数据流全景                                 │
│                                                                    │
│  ┌──────────┐   每5秒    ┌──────────┐   Event     ┌──────────────┐  │
│  │Simulator │ ─────────→ │ Core     │ ─────────→ │    Push      │  │
│  │          │ Generated  │          │ Processed  │              │  │
│  │ 随机生成  │   Event    │ 计算排放  │   Event     │ WebSocket    │  │
│  │ 车流数据  │            │ 生成建议  │             │   广播       │  │
│  │          │            │ 持久化   │             │              │  │
│  └──────────┘            └────┬─────┘            └──────┬───────┘  │
│                               │                         │          │
│                               │ MyBatis 写入             │ 推送     │
│                               ↓                         ↓          │
│                          ┌─────────┐              ┌────────────┐   │
│                          │  MySQL  │              │ Dashboard  │   │
│                          │         │              │   大屏     │   │
│                          └────┬────┘              └─────┬──────┘   │
│                               │                         │          │
│                               │                         │ REST API │
│                               │    ┌──────────┐         │ 主动查询  │
│                               └───→│    API   │←────────┘          │
│                                    │ Controller│                   │
│                                    └──────────┘                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 消息契约迁移说明

- **背景**：为保持六边形架构中 Port 层为业务契约的原则，消息类型常量已从 `green-traffic-common` 移至 `green-traffic-core` 的 Port 层。
- **新位置**：请使用 `com.greentraffic.core.port.output.messaging.TrafficMessageTypes` 作为消息类型契约的权威定义。
- **兼容性**：`green-traffic-common` 中仍保留 `com.greentraffic.common.messaging.TrafficMessageTypes`，但已标记为 `@Deprecated`，建议尽快替换导入。
- **示例替换**：

 旧：

    - `import com.greentraffic.common.messaging.TrafficMessageTypes;`

    新：

    - `import com.greentraffic.core.port.output.messaging.TrafficMessageTypes;`

- **后续步骤**：计划在下一个主版本中移除 `common` 中的旧定义，届时请确保所有模块已切换到新的包。


## 三、数据库设计

### 3.1 ER 图

```
┌─────────────────┐        ┌──────────────────────────┐
│  intersection   │        │       traffic_data       │
├─────────────────┤        ├──────────────────────────┤
│ id (PK)         │◄───────│ intersection_id (FK)     │
│ intersection_id │        │ id (PK)                  │
│ name            │        │ vehicle_count            │
│ district        │        │ avg_wait_time            │
│ lanes           │        │ avg_speed                │
│ has_signal_light│        │ truck_ratio              │
│ location_lat    │        │ carbon_emission          │
│ location_lng    │        │ create_time              │
│ description     │        └──────────┬───────────────┘
└─────────────────┘                   │ 1
                                      │
                                      │ N
                           ┌──────────▼───────────────┐
                           │   optimization_advice     │
                           ├──────────────────────────┤
                           │ id (PK)                  │
                           │ traffic_data_id (FK)     │
                           │ intersection_id          │
                           │ level                    │
                           │ title                    │
                           │ content                  │
                           │ status                   │
                           │ create_time              │
                           └──────────────────────────┘
```

### 3.2 建表 SQL

```sql
-- 路口基础信息表
CREATE TABLE intersection (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(50)  NOT NULL UNIQUE COMMENT '路口唯一编号',
    name            VARCHAR(100) NOT NULL COMMENT '路口名称',
    district        VARCHAR(50)  COMMENT '所属区域',
    lanes           INT          DEFAULT 4 COMMENT '车道数',
    has_signal_light TINYINT(1)  DEFAULT 1 COMMENT '是否有信号灯',
    location_lat    DECIMAL(10, 6) COMMENT '纬度',
    location_lng    DECIMAL(10, 6) COMMENT '经度',
    description     VARCHAR(255) COMMENT '描述',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '路口基础信息表';

-- 交通实时数据表
CREATE TABLE traffic_data (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id  VARCHAR(50) NOT NULL COMMENT '路口编号',
    vehicle_count    INT NOT NULL COMMENT '车辆数',
    avg_wait_time    DECIMAL(5, 1) NOT NULL COMMENT '平均等待时间(秒)',
    avg_speed        DECIMAL(5, 1) NOT NULL COMMENT '平均车速(km/h)',
    truck_ratio      DECIMAL(3, 2) NOT NULL DEFAULT 0 COMMENT '卡车占比',
    carbon_emission  DECIMAL(10, 3) NOT NULL COMMENT '碳排放量(kg)',
    create_time      DATETIME NOT NULL COMMENT '数据产生时间',
    INDEX idx_intersection_time (intersection_id, create_time),
    INDEX idx_create_time (create_time)
) COMMENT '交通实时数据表';

-- 优化建议表
CREATE TABLE optimization_advice (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    traffic_data_id  BIGINT NOT NULL COMMENT '关联交通数据ID',
    intersection_id  VARCHAR(50) NOT NULL COMMENT '路口编号',
    level            VARCHAR(10) NOT NULL COMMENT '建议级别: HIGH/MEDIUM/LOW',
    title            VARCHAR(100) NOT NULL COMMENT '建议标题',
    content          TEXT NOT NULL COMMENT '建议详细内容',
    status           VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/RESOLVED/EXPIRED',
    create_time      DATETIME NOT NULL COMMENT '生成时间',
    INDEX idx_intersection_time (intersection_id, create_time),
    INDEX idx_status (status),
    CONSTRAINT fk_advice_traffic FOREIGN KEY (traffic_data_id) 
        REFERENCES traffic_data(id)
) COMMENT '优化建议表';
```

### 3.3 索引设计说明

| 表 | 索引 | 类型 | 用途 |
|----|------|------|------|
| traffic_data | `idx_intersection_time` | 联合索引 | 查询某路口时间范围数据（最常用） |
| traffic_data | `idx_create_time` | 单列索引 | 按时间全局查询、排行 |
| optimization_advice | `idx_intersection_time` | 联合索引 | 查询某路口建议历史 |
| optimization_advice | `idx_status` | 单列索引 | 按状态筛选待处理建议 |

---

## 四、模块详细设计

### 4.1 green-traffic-common（公共模块）

**模块定位：** 为所有业务模块提供无业务含义的通用能力，是整个系统的"地基"。

**核心类设计：**

| 类名 | 类型 | 职责 |
|------|------|------|
| `ApiResponse<T>` | 泛型类 | 统一 REST 响应体 |
| `PageResult<T>` | 泛型类 | 统一分页响应体 |
| `BusinessException` | 异常类 | 业务规则不满足时抛出 |
| `GlobalExceptionHandler` | `@RestControllerAdvice` | 全局异常拦截 |
| `ErrorCode` | 枚举 | 错误码定义 |
| `DateTimeUtils` | 工具类 | 时间格式化、范围计算 |

**com.greentraffic.common.api.ApiResponse 设计：**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class com.greentraffic.common.api.ApiResponse<T> {
    
    /** 业务状态码：200成功，400参数错误，500系统异常，1xxx业务规则不满足 */
    private int code;
    
    /** 提示信息 */
    private String message;
    
    /** 数据载体 */
    private T data;
    
    /** 响应时间戳 */
    private long timestamp;
    
    /** 请求追踪ID（可选，用于链路追踪） */
    private String traceId;
}
```

**ErrorCode 枚举：**

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数校验失败"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统内部错误"),
    
    INTERSECTION_NOT_FOUND(1001, "路口不存在"),
    TIME_RANGE_TOO_LARGE(1002, "查询时间范围不能超过30天"),
    DATA_NOT_READY(1003, "该路口暂无数据"),
    ADVICE_ALREADY_RESOLVED(1004, "该建议已处理，不可重复操作"),
    SIMULATOR_CONFIG_INVALID(1005, "模拟器配置参数无效");
    
    private final int code;
    private final String defaultMessage;
}
```

### 4.2 green-traffic-model（已移除 / 模型迁移说明）

注：历史上的 `green-traffic-model` 模块已被移除。为保持六边形架构边界清晰，领域模型已迁移到 `green-traffic-core`（领域对象与端口）或 `green-traffic-common`（通用 DTO/VO）。文档中的实体示例保留为参考。

**后续建议：** 将模型定义集中在 `core` 的 `domain` 包中，避免通用模块承担具体业务模型的职责。

#### Intersection（路口实体）

```java
@Entity
@Table(name = "intersection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Intersection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "intersection_id", nullable = false, unique = true, length = 50)
    private String intersectionId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 50)
    private String district;
    
    @Column
    private Integer lanes;
    
    @Column(name = "has_signal_light")
    private Boolean hasSignalLight;
    
    @Column(name = "location_lat", precision = 10, scale = 6)
    private BigDecimal locationLat;
    
    @Column(name = "location_lng", precision = 10, scale = 6)
    private BigDecimal locationLng;
    
    @Column(length = 255)
    private String description;
}
```

#### TrafficData（交通数据实体）

```java
@Entity
@Table(name = "traffic_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "intersection_id", nullable = false, length = 50)
    private String intersectionId;
    
    @Column(name = "vehicle_count", nullable = false)
    private Integer vehicleCount;
    
    @Column(name = "avg_wait_time", nullable = false, precision = 5, scale = 1)
    private BigDecimal avgWaitTime;
    
    @Column(name = "avg_speed", nullable = false, precision = 5, scale = 1)
    private BigDecimal avgSpeed;
    
    @Column(name = "truck_ratio", nullable = false, precision = 3, scale = 2)
    private BigDecimal truckRatio;
    
    @Column(name = "carbon_emission", nullable = false, precision = 10, scale = 3)
    private BigDecimal carbonEmission;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @OneToMany(mappedBy = "trafficData", cascade = CascadeType.ALL)
    private List<OptimizationAdvice> advices;
}
```

#### OptimizationAdvice（优化建议实体）

```java
@Entity
@Table(name = "optimization_advice")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationAdvice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traffic_data_id", nullable = false)
    private TrafficData trafficData;
    
    @Column(name = "intersection_id", nullable = false, length = 50)
    private String intersectionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AdviceLevel level;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AdviceStatus status;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
}
```

**DTO/VO 设计：**

| 类名 | 类型 | 字段说明 |
|------|------|---------|
| `TrafficDataDTO` | DTO | simulator→core 传输用：intersectionId, vehicleCount, avgWaitTime, avgSpeed, truckRatio, timestamp |
| `TrafficDataVO` | VO | 返回前端：包含路口名称、碳排放量、建议列表 |
| `TrendVO` | VO | 趋势图：时间点 + 碳排放值 |
| `RankVO` | VO | 排行：路口名 + 总排放量 + 数据条数 |
| `AdviceVO` | VO | 建议视图：去除 trafficData 关联，直接含 trafficDataId |
| `DashboardInitVO` | VO | 大屏初始化：所有路口最新数据 + 告警列表 |
| `QueryTrafficRequest` | DTO | 查询参数：intersectionId, startTime, endTime, page, size |

### 4.3 green-traffic-simulator（传感器模拟模块）

**模块定位：** 模拟城市路口传感器，定时生成有规律的随机交通数据，是系统的数据源。

**核心类设计：**

```
simulator/
├── TrafficSensorSimulator.java      # 定时任务主类
├── config/
│   └── SimulatorProperties.java     # 可配置参数（@ConfigurationProperties）
├── generator/
│   ├── DataGenerator.java           # 接口
│   ├── UniformDataGenerator.java    # 均匀随机实现
│   ├── PeakHourDataGenerator.java   # 高峰时段实现
│   └── AnomalyDataGenerator.java    # 异常数据实现（演示告警）
└── event/
    └── TrafficDataGeneratedEvent.java  # 数据生成事件
```

**SimulatorProperties 配置类：**

```java
@Data
@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {
    
    /** 定时任务间隔（毫秒），默认5000 */
    private long intervalMs = 5000;
    
    /** 模拟路口数量，默认6 */
    private int intersectionCount = 6;
    
    /** 是否启用高峰时段模型 */
    private boolean peakHourEnabled = true;
    
    /** 是否偶尔生成异常数据 */
    private boolean anomalyEnabled = true;
    
    /** 异常数据生成概率（0~1），默认0.05 */
    private double anomalyProbability = 0.05;
}
```

**application.yml 配置：**

```yaml
simulator:
  interval-ms: 5000
  intersection-count: 6
  peak-hour-enabled: true
  anomaly-enabled: true
  anomaly-probability: 0.05
```

**数据生成策略详解：**

```
┌─────────────────────────────────────────────────────────────┐
│                  TrafficSensorSimulator                      │
│                                                             │
│  @Scheduled(fixedDelayString = "${simulator.interval-ms}")  │
│  public void generateAndPublish() {                         │
│                                                             │
│     // 1. 随机选择一个路口                                    │
│     String intersectionId = randomSelectIntersection();     │
│                                                             │
│     // 2. 判断当前时段                                      │
│     TimeSlot slot = TimeSlot.now();                         │
│     // MORNING_PEAK(7-9) / NOON(11-13) /                    │
│     // EVENING_PEAK(17-19) / NIGHT(23-5) / NORMAL           │
│                                                             │
│     // 3. 根据时段选择数据生成器                               │
│     DataGenerator generator = selectGenerator(slot);         │
│                                                             │
│     // 4. 生成数据                                          │
│     TrafficDataDTO dto = generator.generate(intersectionId);│
│                                                             │
│     // 5. 发布事件                                          │
│     eventPublisher.publish(new TrafficDataGeneratedEvent(dto));│
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

**时段模型设计：**

| 时段 | 时间范围 | 车辆数调整系数 | 等待时间调整 | 速度调整 |
|------|---------|---------------|-------------|---------|
| 早高峰 | 7:00-9:00 | ×1.5 | +20秒 | -15 km/h |
| 午间 | 11:00-13:00 | ×1.2 | +5秒 | -5 km/h |
| 晚高峰 | 17:00-19:00 | ×1.8 | +30秒 | -20 km/h |
| 深夜 | 23:00-5:00 | ×0.3 | -20秒 | +20 km/h |
| 平峰 | 其他 | ×1.0 | 正常 | 正常 |

### 4.4 green-traffic-core（核心业务模块）

**模块定位：** 系统"大脑"，负责碳排放计算、优化建议生成、数据持久化。

**核心类设计：**

```
core/
├── service/
│   ├── TrafficDataProcessor.java       # 数据处理器（事件监听入口）
│   ├── TrafficQueryService.java        # 查询服务
│   ├── AdviceQueryService.java         # 建议查询服务
│   └── IntersectionService.java        # 路口管理服务
├── calculator/
│   ├── CarbonEmissionCalculator.java   # 碳排放计算引擎
│   └── EmissionFactor.java             # 排放因子常量
├── optimizer/
│   ├── TrafficOptimizationEngine.java  # 规则引擎主类
│   ├── rule/
│   │   ├── OptimizationRule.java       # 规则接口
│   │   ├── LongWaitTimeRule.java       # 等待时间过长规则
│   │   ├── LowSpeedRule.java           # 车速过低规则
│   │   ├── HighTruckRatioRule.java     # 卡车占比过高规则
│   │   ├── HighVehicleCountRule.java   # 车流量过大规则
│   │   ├── HighEmissionRule.java       # 碳排放超标规则
│   │   └── PersistentCongestionRule.java # 持续拥堵规则
│   └── RuleContext.java                # 规则上下文
├── repository/
│   ├── TrafficDataRepository.java
│   ├── OptimizationAdviceRepository.java
│   └── IntersectionRepository.java
└── event/
    └── TrafficDataProcessedEvent.java  # 处理完成事件
```

**TrafficDataProcessor 完整流程：**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficDataProcessor {
    
    private final CarbonEmissionCalculator calculator;
    private final TrafficOptimizationEngine optimizer;
    private final TrafficDataRepository trafficDataRepository;
    private final OptimizationAdviceRepository adviceRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Async
    @EventListener
    @Transactional
    public void onTrafficDataGenerated(TrafficDataGeneratedEvent event) {
        TrafficDataDTO dto = event.getTrafficData();
        
        // 第一步：计算碳排放
        BigDecimal emission = calculator.calculate(dto);
        
        // 第二步：生成优化建议
        List<OptimizationAdvice> advices = optimizer.analyze(dto, emission);
        
        // 第三步：组装实体并保存
        TrafficData entity = TrafficData.builder()
                .intersectionId(dto.getIntersectionId())
                .vehicleCount(dto.getVehicleCount())
                .avgWaitTime(dto.getAvgWaitTime())
                .avgSpeed(dto.getAvgSpeed())
                .truckRatio(dto.getTruckRatio())
                .carbonEmission(emission)
                .createTime(dto.getTimestamp())
                .build();
        
        trafficDataRepository.save(entity);
        
        // 第四步：保存建议（关联交通数据）
        advices.forEach(advice -> {
            advice.setTrafficData(entity);
            adviceRepository.save(advice);
        });
        
        // 第五步：发布处理完成事件
        ProcessedResult result = ProcessedResult.builder()
                .trafficData(entity)
                .advices(advices)
                .build();
        eventPublisher.publishEvent(new TrafficDataProcessedEvent(result));
        
        log.debug("处理完成: 路口={}, 碳排放={}kg, 建议数={}", 
                dto.getIntersectionId(), emission, advices.size());
    }
}
```

**碳排放计算引擎详解：**

```java
@Service
public class CarbonEmissionCalculator {
    
    /**
     * 计算路口碳排放量
     * 
     * 公式：
     * 排放量 = 车辆数 × 怠速排放率 × 等待时间(分钟) × 车型加权 × 速度修正
     */
    public BigDecimal calculate(TrafficDataDTO dto) {
        
        // 1. 基础怠速排放率（小汽车，kg/min）
        BigDecimal baseRate = EmissionFactor.CAR_IDLE_RATE; // 0.0307
        
        // 2. 等待时间（分钟）
        BigDecimal waitMinutes = dto.getAvgWaitTime()
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        
        // 3. 基础排放 = 车辆数 × 排放率 × 等待时间
        BigDecimal baseEmission = baseRate
                .multiply(BigDecimal.valueOf(dto.getVehicleCount()))
                .multiply(waitMinutes);
        
        // 4. 车型加权系数 = 1 + truckRatio × (卡车系数 - 1)
        BigDecimal truckWeight = BigDecimal.ONE
                .add(dto.getTruckRatio()
                        .multiply(EmissionFactor.TRUCK_FACTOR.subtract(BigDecimal.ONE)));
        
        // 5. 速度修正系数 = 1.2 - min(speed, 60)/60 × 0.5
        BigDecimal effectiveSpeed = dto.getAvgSpeed().min(BigDecimal.valueOf(60));
        BigDecimal speedCorrection = BigDecimal.valueOf(1.2)
                .subtract(effectiveSpeed
                        .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(0.5)));
        
        // 6. 最终排放量
        BigDecimal totalEmission = baseEmission
                .multiply(truckWeight)
                .multiply(speedCorrection);
        
        return totalEmission.setScale(3, RoundingMode.HALF_UP);
    }
}
```

**规则引擎设计：**

```java
/**
 * 规则接口 - 所有优化规则必须实现
 */
public interface OptimizationRule {
    
    /** 规则优先级（数字越小优先级越高） */
    int getPriority();
    
    /** 评估是否触发规则 */
    boolean evaluate(TrafficDataDTO data, BigDecimal emission);
    
    /** 生成优化建议 */
    OptimizationAdvice buildAdvice(TrafficDataDTO data, BigDecimal emission);
    
    /** 规则编号 */
    String getRuleCode();
    
    /** 规则描述 */
    String getDescription();
}
```

**规则实现示例：**

```java
@Component
@RequiredArgsConstructor
public class LongWaitTimeRule implements OptimizationRule {
    
    private static final BigDecimal WAIT_TIME_THRESHOLD = BigDecimal.valueOf(60);
    
    @Override
    public int getPriority() { return 1; }
    
    @Override
    public String getRuleCode() { return "R1"; }
    
    @Override
    public String getDescription() { return "平均等待时间超过60秒"; }
    
    @Override
    public boolean evaluate(TrafficDataDTO data, BigDecimal emission) {
        return data.getAvgWaitTime().compareTo(WAIT_TIME_THRESHOLD) > 0;
    }
    
    @Override
    public OptimizationAdvice buildAdvice(TrafficDataDTO data, BigDecimal emission) {
        return OptimizationAdvice.builder()
                .intersectionId(data.getIntersectionId())
                .level(AdviceLevel.HIGH)
                .title("信号灯配时建议")
                .content(String.format("%s 平均等待时间 %.1f 秒，超过阈值 %.0f 秒，建议将绿灯周期延长 20%%",
                        data.getIntersectionId(), data.getAvgWaitTime(), WAIT_TIME_THRESHOLD))
                .status(AdviceStatus.ACTIVE)
                .createTime(LocalDateTime.now())
                .build();
    }
}
```

**TrafficOptimizationEngine 引擎主类：**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficOptimizationEngine {
    
    private final List<OptimizationRule> rules;  // Spring 自动注入所有规则实现
    private final Map<String, List<LocalDateTime>> congestionHistory = new ConcurrentHashMap<>();
    
    /**
     * 分析交通数据，返回触发的优化建议列表
     */
    public List<OptimizationAdvice> analyze(TrafficDataDTO data, BigDecimal emission) {
        
        // 按优先级排序
        List<OptimizationRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(OptimizationRule::getPriority))
                .toList();
        
        List<OptimizationAdvice> advices = new ArrayList<>();
        
        for (OptimizationRule rule : sortedRules) {
            try {
                if (rule.evaluate(data, emission)) {
                    OptimizationAdvice advice = rule.buildAdvice(data, emission);
                    advices.add(advice);
                    log.debug("规则 {} 触发: {} - {}", rule.getRuleCode(), 
                            rule.getDescription(), advice.getTitle());
                }
            } catch (Exception e) {
                log.error("规则 {} 执行异常", rule.getRuleCode(), e);
            }
        }
        
        // 更新拥堵历史记录
        updateCongestionHistory(data);
        
        // 检查持续拥堵规则
        if (isPersistentCongestion(data.getIntersectionId())) {
            advices.add(buildPersistentCongestionAdvice(data));
        }
        
        return advices;
    }
}
```

**Repository 设计（复杂查询示例）：**

```java
public interface TrafficDataRepository extends JpaRepository<TrafficData, Long> {
    
    /** 查询某路口最新一条数据 */
    Optional<TrafficData> findTopByIntersectionIdOrderByCreateTimeDesc(String intersectionId);
    
    /** 查询所有路口最新数据 */
    @Query(value = """
            SELECT td.* FROM traffic_data td
            INNER JOIN (
                SELECT intersection_id, MAX(create_time) as max_time
                FROM traffic_data
                GROUP BY intersection_id
            ) latest ON td.intersection_id = latest.intersection_id 
                     AND td.create_time = latest.max_time
            """, nativeQuery = true)
    List<TrafficData> findLatestForAllIntersections();
    
    /** 查询时间范围内趋势数据 */
    List<TrafficData> findByIntersectionIdAndCreateTimeBetweenOrderByCreateTimeAsc(
            String intersectionId, LocalDateTime start, LocalDateTime end);
    
    /** 路口排放排行 */
    @Query("""
            SELECT td.intersectionId, 
                   SUM(td.carbonEmission) as totalEmission,
                   COUNT(td.id) as dataCount
            FROM TrafficData td
            WHERE td.createTime BETWEEN :start AND :end
            GROUP BY td.intersectionId
            ORDER BY totalEmission DESC
            """)
    List<Object[]> getEmissionRanking(@Param("start") LocalDateTime start, 
                                       @Param("end") LocalDateTime end);
    
    /** 24小时各时段平均排放 */
    @Query("""
            SELECT HOUR(td.createTime) as hour,
                   AVG(td.carbonEmission) as avgEmission,
                   MAX(td.carbonEmission) as maxEmission
            FROM TrafficData td
            WHERE td.intersectionId = :intersectionId
              AND td.createTime BETWEEN :start AND :end
            GROUP BY HOUR(td.createTime)
            ORDER BY hour
            """)
    List<Object[]> getHourlyEmission(@Param("intersectionId") String intersectionId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
```

### 4.5 green-traffic-push（实时推送模块）

**模块定位：** 负责将 core 处理完成的数据实时推送到前端，实现大屏"零刷新"更新。

**核心类设计：**

```
push/
├── websocket/
│   ├── WebSocketConfig.java           # WebSocket 配置
│   ├── TrafficWebSocketHandler.java   # 消息处理器
│   └── WebSocketInterceptor.java      # 连接拦截器（握手验证）
├── listener/
│   └── TrafficPushListener.java       # 事件监听器
├── message/
│   ├── WebSocketMessage.java          # 消息封装
│   ├── MessageType.java               # 消息类型枚举
│   └── AlertMessage.java              # 告警消息
└── service/
    └── TrafficPushService.java        # 推送服务
```

**WebSocket 配置：**

```java
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final TrafficWebSocketHandler handler;
    private final WebSocketInterceptor interceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/traffic")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
```

**消息处理器：**

```java
@Component
@Slf4j
public class TrafficWebSocketHandler extends TextWebSocketHandler {
    
    /** 在线连接池：sessionId → session */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        int count = connectedCount.incrementAndGet();
        log.info("WebSocket 连接建立: sessionId={}, 当前连接数={}", session.getId(), count);
        
        // 发送连接确认
        sendToSession(session, WebSocketMessage.confirm(session.getId()));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("收到客户端消息: {}", payload);
        
        // 心跳响应
        if ("PING".equals(payload)) {
            sendToSession(session, WebSocketMessage.heartbeat());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        int count = connectedCount.decrementAndGet();
        log.info("WebSocket 连接关闭: sessionId={}, 状态={}, 当前连接数={}", 
                session.getId(), status, count);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        log.error("WebSocket 传输异常: sessionId={}", session.getId(), exception);
    }
    
    /**
     * 广播消息给所有在线客户端
     */
    public void broadcast(WebSocketMessage message) {
        String json = JSON.toJSONString(message);
        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    log.error("推送消息失败: sessionId={}", sessionId, e);
                    sessions.remove(sessionId);
                }
            } else {
                sessions.remove(sessionId);
            }
        });
    }
    
    /**
     * 获取当前在线连接数
     */
    public int getOnlineCount() {
        return sessions.size();
    }
}
```

**消息类型设计：**

```java
@Getter
@AllArgsConstructor
public enum MessageType {
    
    CONNECTED("连接成功确认"),
    TRAFFIC_DATA("实时交通数据"),
    OPTIMIZATION_ADVICE("优化建议推送"),
    ALERT("高优先级告警"),
    HEARTBEAT("心跳消息");
    
    private final String description;
}
```

**事件监听与推送：**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficPushListener {
    
    private final TrafficWebSocketHandler webSocketHandler;
    
    @Async
    @EventListener
    public void onTrafficDataProcessed(TrafficDataProcessedEvent event) {
        ProcessedResult result = event.getResult();
        
        // 推送交通数据
        WebSocketMessage dataMessage = WebSocketMessage.builder()
                .type(MessageType.TRAFFIC_DATA)
                .payload(TrafficDataVO.fromEntity(result.getTrafficData()))
                .timestamp(System.currentTimeMillis())
                .build();
        webSocketHandler.broadcast(dataMessage);
        
        // 推送优化建议
        result.getAdvices().forEach(advice -> {
            WebSocketMessage adviceMessage = WebSocketMessage.builder()
                    .type(MessageType.OPTIMIZATION_ADVICE)
                    .payload(AdviceVO.fromEntity(advice))
                    .timestamp(System.currentTimeMillis())
                    .build();
            webSocketHandler.broadcast(adviceMessage);
            
            // 高优先级建议同时推送告警
            if (advice.getLevel() == AdviceLevel.HIGH) {
                AlertMessage alert = AlertMessage.builder()
                        .intersectionId(advice.getIntersectionId())
                        .title(advice.getTitle())
                        .content(advice.getContent())
                        .level("HIGH")
                        .triggerTime(advice.getCreateTime())
                        .build();
                
                WebSocketMessage alertMessage = WebSocketMessage.builder()
                        .type(MessageType.ALERT)
                        .payload(alert)
                        .timestamp(System.currentTimeMillis())
                        .build();
                webSocketHandler.broadcast(alertMessage);
            }
        });
    }
}
```

### 4.6 green-traffic-api（Web API 模块）

**模块定位：** 系统唯一对外入口，聚合所有模块，提供 RESTful 接口。

**核心类设计：**

```
api/
├── GreenTrafficApplication.java        # 启动类
├── controller/
│   ├── TrafficController.java          # 交通数据接口
│   ├── AnalysisController.java         # 统计分析接口
│   ├── AdviceController.java           # 优化建议接口
│   ├── IntersectionController.java     # 路口管理接口
│   └── SimulatorController.java        # 模拟器控制接口
├── config/
│   ├── WebMvcConfig.java               # Web MVC 配置
│   ├── OpenApiConfig.java              # Swagger 配置
│   ├── AsyncConfig.java                # 异步配置
│   └── CorsConfig.java                 # 跨域配置
├── aop/
│   └── ApiLogAspect.java               # 接口日志切面
└── init/
    └── DataInitializer.java            # 初始数据加载
```

**启动类：**

```java
@SpringBootApplication(scanBasePackages = "com.greentraffic")
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan
@OpenAPIDefinition(
    info = @Info(
        title = "GreenTraffic API",
        version = "1.0.0",
        description = "城市交通碳排放实时监测与优化系统接口文档"
    )
)
public class GreenTrafficApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GreenTrafficApplication.class, args);
    }
}
```

**TrafficController 示例：**

```java
@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
@Tag(name = "交通数据接口", description = "实时交通数据查询")
public class TrafficController {
    
    private final TrafficQueryService trafficQueryService;
    
    @GetMapping("/latest/{intersectionId}")
    @Operation(summary = "获取路口最新数据", description = "查询指定路口最新一条交通数据")
    @ApiResponses({
        @com.greentraffic.common.api.ApiResponse(responseCode = "200", description = "查询成功"),
        @com.greentraffic.common.api.ApiResponse(responseCode = "1001", description = "路口不存在")
    })
    public com.greentraffic.common.api.ApiResponse<TrafficDataVO> getLatest(
            @Parameter(description = "路口编号", example = "INTERSECTION_01")
            @PathVariable String intersectionId) {
        
        TrafficDataVO vo = trafficQueryService.getLatestByIntersection(intersectionId);
        return com.greentraffic.common.api.ApiResponse.success(vo);
    }
    
    @GetMapping("/latest/all")
    @Operation(summary = "获取所有路口最新数据", description = "大屏初始化时调用")
    public com.greentraffic.common.api.ApiResponse<List<TrafficDataVO>> getLatestAll() {
        List<TrafficDataVO> list = trafficQueryService.getLatestForAllIntersections();
        return com.greentraffic.common.api.ApiResponse.success(list);
    }
    
    @GetMapping("/trend")
    @Operation(summary = "获取趋势数据", description = "查询指定路口在时间范围内的排放趋势")
    public com.greentraffic.common.api.ApiResponse<List<TrendVO>> getTrend(
            @Parameter(description = "路口编号") 
            @RequestParam String intersectionId,
            
            @Parameter(description = "时间范围（分钟），最大180分钟") 
            @RequestParam(defaultValue = "30") @Min(1) @Max(180) int minutes) {
        
        List<TrendVO> trend = trafficQueryService.getTrend(intersectionId, minutes);
        return com.greentraffic.common.api.ApiResponse.success(trend);
    }
}
```

**AnalysisController 示例：**

```java
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "统计分析接口", description = "碳排放统计与分析")
public class AnalysisController {
    
    private final AnalysisService analysisService;
    
    @GetMapping("/ranking")
    @Operation(summary = "碳排放排行", description = "指定日期各路口总碳排放排行")
    public com.greentraffic.common.api.ApiResponse<List<RankVO>> getRanking(
            @Parameter(description = "统计日期，格式 yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        List<RankVO> ranking = analysisService.getEmissionRanking(date);
        return com.greentraffic.common.api.ApiResponse.success(ranking);
    }
    
    @GetMapping("/hourly")
    @Operation(summary = "24小时排放分布", description = "某路口某天各时段排放统计")
    public com.greentraffic.common.api.ApiResponse<List<HourlyEmissionVO>> getHourly(
            @RequestParam String intersectionId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        List<HourlyEmissionVO> hourly = analysisService.getHourlyEmission(intersectionId, date);
        return com.greentraffic.common.api.ApiResponse.success(hourly);
    }
    
    @GetMapping("/congestion")
    @Operation(summary = "拥堵统计", description = "统计各路口触发拥堵规则的次数")
    public com.greentraffic.common.api.ApiResponse<List<CongestionStatVO>> getCongestionStat(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        List<CongestionStatVO> stats = analysisService.getCongestionStatistics(date);
        return com.greentraffic.common.api.ApiResponse.success(stats);
    }
}
```

**AOP 日志切面：**

```java
@Aspect
@Component
@Slf4j
public class ApiLogAspect {
    
    @Around("execution(* com.greentraffic.api.controller..*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        
        long startTime = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        
        // 过滤敏感参数（如文件、request/response）
        String params = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof HttpServletResponse))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        log.info("[API-IN] {} | 参数: {}", method, params);
        
        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("[API-OUT] {} | 耗时: {}ms | 结果: {}", method, costTime, 
                    result != null ? "success" : "null");
            return result;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("[API-ERROR] {} | 耗时: {}ms | 异常: {}", method, costTime, e.getMessage());
            throw e;
        }
    }
}
```

**DataInitializer 初始化：**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {
    
    private final IntersectionRepository intersectionRepository;
    
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (intersectionRepository.count() > 0) {
            log.info("路口数据已存在，跳过初始化");
            return;
        }
        
        List<Intersection> defaultIntersections = Arrays.asList(
                Intersection.builder()
                        .intersectionId("INTERSECTION_01")
                        .name("人民路与解放路交叉口")
                        .district("城关区")
                        .lanes(6)
                        .hasSignalLight(true)
                        .locationLat(new BigDecimal("36.061200"))
                        .locationLng(new BigDecimal("103.834300"))
                        .build(),
                // ... 更多默认路口
        );
        
        intersectionRepository.saveAll(defaultIntersections);
        log.info("初始化 {} 个默认路口", defaultIntersections.size());
    }
}
```

### 4.7 green-traffic-dashboard（前端大屏模块）

**模块定位：** 提供静态监控大屏页面，通过 WebSocket + REST API 与后端交互。

**文件结构：**

```
dashboard/src/main/resources/static/
├── index.html              # 主页面
├── css/
│   ├── dashboard.css       # 大屏样式
│   └── dark-theme.css      # 暗色主题
├── js/
│   ├── app.js              # 主逻辑入口
│   ├── websocket-client.js # WebSocket 客户端封装
│   ├── api-client.js       # REST API 调用封装
│   ├── charts.js           # ECharts 图表初始化与更新
│   └── constants.js        # 常量定义
└── assets/
    ├── logo.png
    └── bg.png
```

**index.html 结构：**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>城市交通碳排放实时监测平台</title>
    <link rel="stylesheet" href="css/dashboard.css">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
</head>
<body>
    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
        <h1>🌿 城市交通碳排放实时监测平台</h1>
        <div class="header-info">
            <span id="current-time"></span>
            <span id="connection-status" class="status-disconnected">未连接</span>
            <span id="online-count">在线: 0</span>
        </div>
    </header>
    
    <!-- 主体内容 -->
    <main class="dashboard-main">
        <!-- 第一行：路口卡片 + 告警列表 -->
        <section class="row-1">
            <div id="intersection-cards" class="cards-container">
                <!-- 动态生成路口卡片 -->
            </div>
            <div id="alert-panel" class="alert-panel">
                <h3>实时告警</h3>
                <ul id="alert-list"></ul>
            </div>
        </section>
        
        <!-- 第二行：趋势图 -->
        <section class="row-2">
            <div id="trend-chart" class="chart-container"></div>
        </section>
        
        <!-- 第三行：排行 + 热力图 -->
        <section class="row-3">
            <div id="ranking-chart" class="chart-container"></div>
            <div id="hourly-chart" class="chart-container"></div>
        </section>
    </main>
    
    <script src="js/constants.js"></script>
    <script src="js/api-client.js"></script>
    <script src="js/websocket-client.js"></script>
    <script src="js/charts.js"></script>
    <script src="js/app.js"></script>
</body>
</html>
```

**WebSocket 客户端封装：**

```javascript
// websocket-client.js
class TrafficWebSocketClient {
    
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.listeners = new Map();
        this.heartbeatTimer = null;
    }
    
    connect() {
        this.ws = new WebSocket(this.url);
        
        this.ws.onopen = () => {
            console.log('WebSocket 连接成功');
            this.reconnectAttempts = 0;
            this.updateStatus('connected');
            this.startHeartbeat();
        };
        
        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.dispatch(message);
        };
        
        this.ws.onclose = () => {
            console.log('WebSocket 连接关闭');
            this.updateStatus('disconnected');
            this.stopHeartbeat();
            this.tryReconnect();
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket 错误', error);
        };
    }
    
    on(type, callback) {
        if (!this.listeners.has(type)) {
            this.listeners.set(type, []);
        }
        this.listeners.get(type).push(callback);
    }
    
    dispatch(message) {
        const callbacks = this.listeners.get(message.type);
        if (callbacks) {
            callbacks.forEach(cb => cb(message.payload, message.timestamp));
        }
    }
    
    startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                this.ws.send('PING');
            }
        }, 30000);
    }
    
    stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }
    
    tryReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            setTimeout(() => this.connect(), this.reconnectDelay);
        }
    }
    
    updateStatus(status) {
        const statusEl = document.getElementById('connection-status');
        if (statusEl) {
            statusEl.textContent = status === 'connected' ? '已连接' : '未连接';
            statusEl.className = status === 'connected' 
                ? 'status-connected' : 'status-disconnected';
        }
    }
}
```

**主逻辑 app.js：**

```javascript
// app.js
document.addEventListener('DOMContentLoaded', () => {
    
    // 1. 初始化 ECharts
    const trendChart = initTrendChart(document.getElementById('trend-chart'));
    const rankingChart = initRankingChart(document.getElementById('ranking-chart'));
    const hourlyChart = initHourlyChart(document.getElementById('hourly-chart'));
    
    // 2. 加载初始数据
    loadInitialData();
    
    // 3. 建立 WebSocket 连接
    const wsClient = new TrafficWebSocketClient('ws://localhost:8080/ws/traffic');
    
    wsClient.on('TRAFFIC_DATA', (data) => {
        updateIntersectionCard(data);
        trendChart.addData(data);
    });
    
    wsClient.on('OPTIMIZATION_ADVICE', (advice) => {
        addAdviceToAlertList(advice);
    });
    
    wsClient.on('ALERT', (alert) => {
        showAlert(alert);
        flashIntersectionCard(alert.intersectionId);
    });
    
    wsClient.connect();
    
    // 4. 定时刷新时间显示
    setInterval(() => {
        document.getElementById('current-time').textContent = 
            new Date().toLocaleString('zh-CN');
    }, 1000);
});

async function loadInitialData() {
    try {
        const [latestData, ranking, hourly] = await Promise.all([
            ApiClient.get('/api/traffic/latest/all'),
            ApiClient.get('/api/analysis/ranking?date=' + today()),
            ApiClient.get('/api/analysis/hourly?intersectionId=INTERSECTION_01&date=' + today())
        ]);
        
        renderIntersectionCards(latestData.data);
        renderRankingChart(ranking.data);
        renderHourlyChart(hourly.data);
    } catch (error) {
        console.error('加载初始数据失败', error);
    }
}
```

---

## 五、核心业务流程

### 5.1 实时数据采集与处理流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        实时数据采集与处理流程                              │
└─────────────────────────────────────────────────────────────────────────┘

时间轴（每5秒循环一次）：

T+0s    Simulator 定时任务触发
        │
        ▼
T+0.1s  随机选择路口 → 判断时段 → 选择数据生成器 → 生成 TrafficDataDTO
        │
        ▼
T+0.2s  发布 TrafficDataGeneratedEvent
        │
        ▼
T+0.3s  Core 模块 @EventListener 异步接收事件
        │
        ├──→ 调用 CarbonEmissionCalculator.calculate(dto)
        │        计算碳排放量（约 0.1ms）
        │
        ├──→ 调用 TrafficOptimizationEngine.analyze(dto, emission)
        │        遍历所有规则，评估是否触发（约 0.5ms）
        │        生成 OptimizationAdvice 列表
        │
        ├──→ 保存 TrafficData 到 MySQL（约 10ms）
        │
        ├──→ 保存 OptimizationAdvice 列表到 MySQL（约 20ms）
        │
        └──→ 发布 TrafficDataProcessedEvent
                  │
                  ▼
T+0.5s  Push 模块 @EventListener 异步接收事件
        │
        ├──→ 封装 TRAFFIC_DATA 消息 → WebSocket 广播
        ├──→ 封装 OPTIMIZATION_ADVICE 消息 → WebSocket 广播
        └──→ 如有 HIGH 级别建议 → 封装 ALERT 消息 → 广播
                  │
                  ▼
T+0.6s  Dashboard 收到 WebSocket 消息
        │
        ├──→ 更新路口卡片数据
        ├──→ 趋势图追加数据点
        ├──→ 告警列表插入新告警
        └──→ HIGH 告警触发卡片闪烁动画
```

### 5.2 优化建议生命周期

```
┌─────────────────────────────────────────────────────────────┐
│                    优化建议生命周期                           │
└─────────────────────────────────────────────────────────────┘

                    ┌──────────────┐
                    │  数据触发    │
                    │ 规则引擎评估 │
                    └──────┬───────┘
                           │ 满足条件
                           ▼
                    ┌──────────────┐
                    │    ACTIVE    │ ←── 新建建议，状态为生效中
                    │  （生效中）   │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ 自动推送  │ │ 管理查看  │ │ 定时检查  │
        │ 到大屏    │ │ 详情     │ │ 是否过期  │
        └──────────┘ └────┬─────┘ └────┬─────┘
                          │            │
                          ▼            ▼
                    ┌──────────┐ ┌──────────┐
                    │ 标记处理  │ │ 自动过期  │
                    │          │ │ （超过24h）│
                    └────┬─────┘ └────┬─────┘
                         │            │
                         ▼            ▼
                    ┌──────────┐ ┌──────────┐
                    │ RESOLVED │ │ EXPIRED  │
                    │ （已解决） │ │ （已过期） │
                    └──────────┘ └──────────┘
```

### 5.3 异常处理流程

```
┌─────────────────────────────────────────────────────────────┐
│                     异常处理流程                              │
└─────────────────────────────────────────────────────────────┘

场景一：业务异常（如查询不存在的路口）
─────────────────────────────────────
Service 层:  throw new BusinessException(ErrorCode.INTERSECTION_NOT_FOUND)
        ↓
GlobalExceptionHandler: @ExceptionHandler(BusinessException.class)
        ↓
返回: com.greentraffic.common.api.ApiResponse(1001, "路口不存在", null, timestamp)
        ↓
前端: 拦截响应，提示"路口不存在"

场景二：参数校验异常
─────────────────────────────────────
Controller: @Valid @RequestBody QueryTrafficRequest
        ↓
Spring 校验失败，抛出 MethodArgumentNotValidException
        ↓
GlobalExceptionHandler: @ExceptionHandler(MethodArgumentNotValidException.class)
        ↓
提取字段错误信息: "查询时间范围不能超过30天"
        ↓
返回: com.greentraffic.common.api.ApiResponse(400, "参数校验失败: 查询时间范围不能超过30天", null, timestamp)

场景三：系统异常（如数据库连接失败）
─────────────────────────────────────
任意层: 抛出 RuntimeException（非业务异常）
        ↓
GlobalExceptionHandler: @ExceptionHandler(Exception.class)
        ↓
打印完整堆栈日志（方便排查）
        ↓
返回: com.greentraffic.common.api.ApiResponse(500, "系统内部错误，请稍后重试", null, timestamp)
        ↓
前端: 提示"系统繁忙，请稍后重试"（不暴露内部错误细节）
```

---

## 六、接口文档（Swagger 摘要）

### 6.1 交通数据接口 `/api/traffic`

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| GET | `/latest/{intersectionId}` | Path: 路口编号 | `TrafficDataVO` | 单路口最新数据 |
| GET | `/latest/all` | 无 | `List<TrafficDataVO>` | 所有路口最新数据 |
| GET | `/trend` | Query: intersectionId, minutes | `List<TrendVO>` | 趋势数据 |
| GET | `/history` | Query: intersectionId, startTime, endTime, page, size | `PageResult<TrafficDataVO>` | 分页历史数据 |

### 6.2 统计分析接口 `/api/analysis`

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| GET | `/ranking` | Query: date | `List<RankVO>` | 日排放排行 |
| GET | `/hourly` | Query: intersectionId, date | `List<HourlyEmissionVO>` | 24小时分布 |
| GET | `/congestion` | Query: date | `List<CongestionStatVO>` | 拥堵统计 |
| GET | `/summary` | Query: date | `SummaryVO` | 日汇总数据 |

### 6.3 优化建议接口 `/api/advice`

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| GET | `/list` | Query: status, level, intersectionId, page, size | `PageResult<AdviceVO>` | 建议分页列表 |
| GET | `/{id}` | Path: 建议ID | `AdviceVO` | 建议详情 |
| PUT | `/{id}/resolve` | Path: 建议ID | `void` | 标记已解决 |
| GET | `/statistics` | Query: date | `AdviceStatVO` | 建议统计 |

### 6.4 系统管理接口 `/api/system`

| 方法 | 路径 | 参数 | 返回 | 说明 |
|------|------|------|------|------|
| GET | `/intersection/list` | 无 | `List<IntersectionVO>` | 路口列表 |
| POST | `/simulator/trigger` | Body: intersectionId(可选) | `TrafficDataDTO` | 手动触发模拟 |
| PUT | `/simulator/interval` | Body: intervalMs | `void` | 修改模拟频率 |
| GET | `/simulator/status` | 无 | `SimulatorStatusVO` | 模拟器状态 |
| GET | `/ws/online-count` | 无 | `int` | WebSocket在线数 |

---

## 七、配置文件

### 7.1 application.yml（开发环境）

```yaml
spring:
  application:
    name: green-traffic
  
  datasource:
    url: jdbc:mysql://localhost:3306/green_traffic?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

server:
  port: 8080
  servlet:
    context-path: /

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

simulator:
  interval-ms: 5000
  intersection-count: 6
  peak-hour-enabled: true
  anomaly-enabled: true
  anomaly-probability: 0.05

logging:
  level:
    com.greentraffic: debug
    org.hibernate.SQL: warn
  file:
    name: logs/green-traffic.log
```

### 7.2 application-demo.yml（演示环境，使用 H2）

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:green_traffic;DB_CLOSE_DELAY=-1
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
      path: /h2-console

simulator:
  interval-ms: 3000  # 演示时加快频率，3秒一次
```

---

## 八、部署与运行

### 8.1 环境要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Spring Boot 3.x 要求 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 生产/开发环境 |
| 浏览器 | Chrome 90+ / Edge 90+ | 支持 WebSocket |
| IDE | IDEA 2023+ | 推荐开发工具 |

### 8.2 运行步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-username/green-traffic.git
cd green-traffic

# 2. 创建数据库
mysql -u root -p
CREATE DATABASE green_traffic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 修改数据库配置
# 编辑 green-traffic-api/src/main/resources/application.yml
# 修改 spring.datasource.username / password

# 4. 构建项目
mvn clean package -DskipTests

# 5. 启动项目
java -jar green-traffic-api/target/green-traffic-api-1.0.0.jar

# 或者开发模式启动
mvn spring-boot:run -pl green-traffic-api

# 或者使用 H2 演示模式（无需 MySQL）
mvn spring-boot:run -pl green-traffic-api -Dspring-boot.run.profiles=demo
```

### 8.3 访问地址

| 资源 | 地址 | 说明 |
|------|------|------|
| 监控大屏 | `http://localhost:8080/` | 实时数据展示 |
| Swagger 文档 | `http://localhost:8080/swagger-ui.html` | 接口文档与调试 |
| H2 控制台 | `http://localhost:8080/h2-console` | 演示模式数据库 |
| WebSocket | `ws://localhost:8080/ws/traffic` | 实时推送连接 |

---

## 九、教学演示指南

### 9.1 演示前准备

```bash
# 1. 使用演示模式启动（H2数据库，无需MySQL）
mvn spring-boot:run -pl green-traffic-api -Dspring-boot.run.profiles=demo

# 2. 等待启动完成，观察日志
# 日志中出现以下内容表示启动成功：
#   - 初始化 N 个默认路口
#   - Tomcat started on port 8080
#   - Simulator 定时任务已注册
```

### 9.2 演示顺序建议

| 步骤 | 操作 | 讲解要点 | 预期效果 |
|------|------|---------|---------|
| **1** | 打开 Swagger | 介绍接口设计规范 | 看到分组清晰的接口文档 |
| **2** | 打开大屏首页 | 介绍前后端分离架构 | 初始数据加载，图表渲染 |
| **3** | 观察实时更新 | 讲解 WebSocket vs 轮询 | 每 5 秒数据自动刷新 |
| **4** | 调用趋势接口 | 讲解 JPA 查询 | 返回 JSON 趋势数据 |
| **5** | 调用排行接口 | 讲解聚合查询 | 返回各路口排名 |
| **6** | 手动触发模拟 | 讲解事件驱动 | 立即产生一条新数据 |
| **7** | 标记建议已解决 | 讲解状态流转 | 建议状态变为 RESOLVED |
| **8** | 查看日志文件 | 讲解 AOP 日志 | 看到完整的调用日志 |
| **9** | 触发异常场景 | 讲解全局异常 | 返回统一错误格式 |
| **10** | 修改模拟器频率 | 讲解配置热更新 | 数据产生速度变化 |

### 9.3 效果演示场景

**场景一：高峰时段拥堵**

```
操作：将系统时间调整到 7:00-9:00 之间（或修改高峰判断逻辑）
预期：车流量增加、等待时间变长、速度降低
结果：HIGH 级别告警触发，大屏卡片闪烁，建议列表新增多条信号灯配时建议
```

**场景二：异常数据告警**

```
操作：等待模拟器随机生成异常数据（概率5%）
预期：某路口突然出现极端数据（如80辆车、等待95秒）
结果：碳排放量飙升，触发 HIGH 告警，大屏弹窗提示
```

**场景三：历史趋势分析**

```
操作：调用 /api/analysis/trend?intersectionId=INTERSECTION_01&minutes=30
预期：返回近30分钟排放趋势
结果：折线图展示排放波动，可以关联到高峰时段
```

---

## 十、扩展方向

### 10.1 技术扩展

| 方向 | 具体方案 | 教学价值 |
|------|---------|---------|
| **消息队列** | 引入 RabbitMQ/Kafka 替代 Spring Event | 理解异步解耦、削峰填谷 |
| **缓存** | Redis 缓存路口基础信息、实时排行榜 | 理解缓存一致性、过期策略 |
| **分布式** | 模拟器拆分独立服务，通过 MQ 通信 | 理解微服务架构 |
| **容器化** | Docker Compose 一键启动 | 理解容器化部署 |
| **实时计算** | Flink/Spark Streaming 计算碳排放 | 理解大数据实时计算 |
| **AI 预测** | 集成机器学习模型预测未来排放 | 理解 AI 应用场景 |

### 10.2 功能扩展

| 方向 | 具体方案 | 说明 |
|------|---------|------|
| **电子地图** | 接入高德/百度地图 API | 路口位置可视化 |
| **3D 大屏** | 使用 Three.js / DataV | 更酷炫的展示效果 |
| **移动端** | 开发小程序/App | 随时随地查看数据 |
| **报表导出** | 生成 Excel/PDF 日报 | 管理汇报使用 |
| **多城市支持** | 添加城市维度 | 多城市对比分析 |
| **用户权限** | Spring Security + JWT | 不同角色看到不同数据 |

### 10.3 规则引擎扩展

```
当前实现：代码中的规则类（简单直观）
扩展方向一：引入 Drools 规则引擎
  - 规则配置化，无需修改代码
  - 业务人员可以配置规则
  - 支持复杂规则组合

扩展方向二：引入 EasyRules
  - 轻量级规则引擎
  - 注解式规则定义
  - 适合教学演示

扩展方向三：数据库配置规则
  - 规则存储在数据库
  - 动态加载与更新
  - 规则执行日志
```

---

## 十一、项目统计

| 指标 | 数值 |
|------|------|
| Maven 模块数 | 7 个 |
| Java 源文件（预估） | 60+ 个 |
| 核心代码行数（预估） | 4000+ 行 |
| REST API 接口数 | 20+ 个 |
| 数据库表 | 3 张 |
| 规则引擎规则数 | 6 条 |
| WebSocket 消息类型 | 5 种 |
| 前端图表 | 4 种 |

---

## 十二、总结

### 12.1 项目特色

```
✅ 主题新颖：结合"双碳"热点，有社会意义
✅ 架构清晰：多模块拆分，职责分明
✅ 技术全面：覆盖 Spring Boot 核心知识点
✅ 实时性强：WebSocket 推送，非纯 CRUD
✅ 可扩展性：预留多个扩展方向
✅ 教学友好：每个模块可独立讲解演示
✅ 可视化强：大屏效果直观，演示效果好
```

### 12.2 知识点覆盖

| Spring Boot 核心知识点 | 项目中的应用 |
|----------------------|-------------|
| 自动配置 | `@SpringBootApplication`、`@EnableScheduling` |
| 依赖注入 | 构造器注入、`@RequiredArgsConstructor` |
| 定时任务 | `@Scheduled`、动态修改间隔 |
| 事件机制 | `ApplicationEventPublisher`、`@EventListener` |
| 异步处理 | `@Async`、线程池配置 |
| 数据访问 | Spring Data JPA、JPQL、原生 SQL |
| 事务管理 | `@Transactional` |
| 参数校验 | JSR-303 注解、`@Valid` |
| 全局异常 | `@RestControllerAdvice` |
| AOP | 日志切面 |
| WebSocket | 连接管理、消息广播 |
| 配置管理 | `@ConfigurationProperties` |
| 接口文档 | springdoc-openapi |
| 多环境 | Profile 切换（MySQL/H2） |

这份文档可以作为项目开发的教学讲义使用，也可以作为项目 README 的基础。如需进一步细化某个模块的代码实现或补充更多内容，可以继续探讨。



































# GreenTraffic - 城市交通碳排放实时监测与优化系统

## 介绍

### 项目名称

**GreenTraffic - 城市交通碳排放实时监测与优化系统**

英文名简短易记，Green（绿色/环保）+ Traffic（交通），直接点题。

## 模块依赖关系

```text
green-traffic-common  ← 被所有模块依赖
              ↑
green-traffic-model   ← 被 simulator / core / push / api 依赖
              ↑
┌─────────────┼──────────────┬──────────────┐
│             │              │              │
simulator    core          push            api
              ↑              ↑              ↑
              └───────  ─────┴──────────────┘
                    最终由 api 聚合启动
```

## 模块职责一句话总结

| **模块职责** | |
| --- | --- |
| **common** | 通用返回体、异常、工具 |
| **model** | 纯 POJO、DTO、枚举，无业务逻辑 |
| **simulator** | 模拟传感器，定时产生车流数据 |
| **core** | 碳排放计算、优化建议、数据持久化 |
| **push** | WebSocket 实时推送 |
| **api** | 控制器、配置、启动入口，聚合各模块 |
| **dashboard** | 静态前端大屏页面 |

## 技术栈

- Java 17
- Spring Boot 4.0.7
- Spring Web
- Spring Data JPA
- WebSocket
- Spring Validation
- Spring Actuator
- Maven 多模块项目

## Maven 依赖管理说明

本项目**不使用** `spring-boot-starter-parent` 作为 `<parent>`。

根 POM 仅作为 Maven 聚合父工程，并通过：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

统一管理 Spring Boot 及相关依赖版本。

## 启动方式

在项目根目录执行：

```bash
mvn clean package
mvn -pl green-traffic-api -am spring-boot:run
```

也可以在 IDE 中直接运行：

`green-traffic-api/src/main/java/com/greentraffic/api/GreenTrafficApplication.java`

## 说明

当前目录已经按照 common、model、simulator、core、push、api 六个 Maven module 搭建。
其中 API 是最终启动模块；simulator、core、push 均作为业务能力模块被 API 聚合。

后续可以继续补充：
- JPA Entity 与 Repository
- 统一 Result / ErrorCode
- GlobalExceptionHandler
- 车辆/车型/告警枚举
- 碳排放计算公式与策略
- WebSocket 推送
- Swagger/OpenAPI
- 数据库配置与 Flyway/Liquibase
- dashboard 前端工程
