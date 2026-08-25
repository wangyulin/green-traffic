# GreenTraffic 🌿

> 城市交通碳排放实时监测与智能优化系统

GreenTraffic 是一个基于 **Java / Spring Boot** 构建的城市交通碳排放监测与优化原型系统。

项目通过模拟城市道路路口的实时交通数据，完成**交通数据采集、碳排放计算、交通状态分析、优化建议生成、数据持久化以及 WebSocket 实时推送**，并通过可视化 Dashboard 对城市路口交通与碳排放状态进行实时展示。

项目定位于**智慧交通、绿色交通、交通数字化以及低碳城市**相关场景，同时也可作为 Spring Boot 多模块架构、事件驱动、WebSocket、规则引擎和数据分析等技术的学习与实践项目。

---

## ✨ 项目特点

- 🚦 **实时交通数据模拟**：模拟城市多个路口的车辆数量、平均速度、等待时间、车辆类型比例等数据。
- 🌱 **交通碳排放计算**：根据交通运行状态计算路口碳排放量。
- 🧠 **规则驱动的优化建议**：根据拥堵、低速、高车流量、高卡车占比、高排放等条件生成优化建议。
- ⚡ **事件驱动架构**：通过 Spring Application Event 解耦数据生成、业务处理和实时推送。
- 📡 **WebSocket 实时推送**：交通数据和高等级告警可以实时推送至前端 Dashboard。
- 📊 **交通数据分析**：支持趋势、排行、小时分布、拥堵统计和汇总数据查询。
- 🖥️ **可视化监控大屏**：基于 HTML + ECharts 构建实时交通碳排放 Dashboard。
- 🧩 **Maven 多模块架构**：按照公共能力、领域模型、核心业务、接口、推送和模拟器等职责拆分。
- 🔧 **适合二次开发**：核心业务与基础设施相对解耦，可进一步接入真实交通感知设备、Kafka、Redis、GIS、AI 模型等。

---

## 🏗️ 系统架构

当前项目采用 Maven 多模块设计，主要模块如下：

```text
green-traffic
│
├── green-traffic-common
│   └── 公共能力
│       ├── 统一响应
│       ├── 异常处理
│       ├── 错误码
│       └── 通用工具
│
├── green-traffic-model
│   └── 数据模型
│       ├── Intersection
│       ├── TrafficData
│       └── OptimizationAdvice
│
├── green-traffic-simulator
│   └── 交通数据模拟
│       ├── 定时任务
│       ├── 随机数据生成
│       └── 高峰/平峰时段模型
│
├── green-traffic-core
│   └── 核心业务
│       ├── 碳排放计算
│       ├── 优化规则引擎
│       ├── 交通数据处理
│       ├── 查询服务
│       └── 数据持久化
│
├── green-traffic-push
│   └── 实时消息推送
│       ├── WebSocket
│       ├── 消息广播
│       ├── 心跳
│       └── 实时告警
│
├── green-traffic-api
│   └── REST API
│       ├── 交通数据接口
│       ├── 分析接口
│       ├── 优化建议接口
│       └── 系统管理接口
│
├── green-traffic-dashboard
│   └── Web 可视化大屏
│       ├── 路口实时状态
│       ├── 告警信息
│       ├── 排放趋势
│       ├── 排放排行
│       └── 小时分布
│
├── green-traffic-infrastructure
│   └── 基础设施相关能力
│
├── green-traffic-doc
│   └── 项目文档
│
└── sumo-work
    └── SUMO 交通仿真相关工作目录
```

整体数据链路：

```text
┌──────────────────┐
│ Traffic Simulator│
│   交通数据模拟     │
└────────┬─────────┘
         │
         │ TrafficDataGeneratedEvent
         ▼
┌─────────────────────────────┐
│          Core               │
│                             │
│  碳排放计算 → 规则分析       │
│       ↓          ↓          │
│    数据持久化   优化建议      │
└────────────┬────────────────┘
             │
             │ TrafficDataProcessedEvent
             ▼
┌─────────────────────────────┐
│            Push             │
│          WebSocket          │
└────────────┬────────────────┘
             │
             │ 实时消息
             ▼
┌─────────────────────────────┐
│         Dashboard           │
│                             │
│  路口状态 │ 趋势 │ 排行      │
│  实时告警 │ 小时分析         │
└─────────────────────────────┘
```

---

## 🔄 核心业务流程

系统当前以模拟交通数据作为数据源，典型处理流程如下：

```text
                    每 5 秒
                       │
                       ▼
             ┌──────────────────┐
             │  Simulator        │
             │  生成交通数据      │
             └────────┬─────────┘
                      │
                      ▼
          TrafficDataGeneratedEvent
                      │
                      ▼
             ┌──────────────────┐
             │      Core        │
             │                  │
             │  ① 碳排放计算     │
             │  ② 规则分析       │
             │  ③ 生成优化建议    │
             │  ④ 数据持久化      │
             └────────┬─────────┘
                      │
                      ▼
          TrafficDataProcessedEvent
                      │
                      ▼
             ┌──────────────────┐
             │       Push       │
             │    WebSocket     │
             └────────┬─────────┘
                      │
                      ▼
             ┌──────────────────┐
             │    Dashboard     │
             │                  │
             │  实时更新路口状态  │
             │  更新趋势图        │
             │  展示优化建议      │
             │  展示高等级告警     │
             └──────────────────┘
```

这种设计使数据生产、业务处理和前端推送之间保持相对独立。

---

## 🧠 核心功能

### 1. 交通数据模拟

当前项目通过 Simulator 模块模拟城市交通感知数据。

主要数据包括：

| 数据 | 说明 |
|---|---|
| `vehicleCount` | 车辆数量 |
| `avgWaitTime` | 平均等待时间 |
| `avgSpeed` | 平均车速 |
| `truckRatio` | 卡车占比 |
| `carbonEmission` | 碳排放量 |
| `createTime` | 数据产生时间 |

模拟器根据当前时间段调整交通数据模型，目前包含：

- 早高峰：07:00–09:00
- 午间：11:00–13:00
- 晚高峰：17:00–19:00
- 深夜：23:00–05:00
- 平峰：其他时间

因此系统能够形成具有一定时间特征的交通运行数据，而不是简单生成完全随机的数据。

---

### 2. 碳排放计算

Core 模块中的 `CarbonEmissionCalculator` 负责根据交通数据计算碳排放量。

核心处理链路：

```text
TrafficDataDTO
      │
      ▼
CarbonEmissionCalculator
      │
      ▼
carbonEmission
      │
      ├── 保存数据库
      │
      └── 参与优化规则分析
```

碳排放数据最终可以用于：

- 路口碳排放监测
- 时间趋势分析
- 路口排放排行
- 拥堵与碳排放关联分析
- 优化建议触发

---

### 3. 交通优化规则引擎

项目目前采用规则驱动方式生成交通优化建议。

核心规则包括：

```text
OptimizationRule
│
├── LongWaitTimeRule
│   └── 平均等待时间过长
│
├── LowSpeedRule
│   └── 平均车速过低
│
├── HighTruckRatioRule
│   └── 卡车占比过高
│
├── HighVehicleCountRule
│   └── 车流量过大
│
├── HighEmissionRule
│   └── 碳排放过高
│
└── PersistentCongestionRule
    └── 持续拥堵
```

规则引擎通过统一的 `OptimizationRule` 接口组织不同规则，使后续增加新的交通优化规则时不需要修改核心处理流程。

---

### 4. 实时 WebSocket 推送

Push 模块提供 WebSocket 实时通信能力。

当前 WebSocket Endpoint：

```text
/ws/traffic
```

前端连接建立后，系统可以广播实时交通消息。

主要消息类型包括：

```text
TRAFFIC_DATA
OPTIMIZATION_ADVICE
ALERT
```

其中：

- `TRAFFIC_DATA`：实时交通数据
- `OPTIMIZATION_ADVICE`：交通优化建议
- `ALERT`：高等级告警

前端同时具备：

- 心跳机制
- 自动重连
- 连接状态显示

---

### 5. Dashboard 可视化

当前 Dashboard 为原生 HTML + CSS + JavaScript + ECharts 实现。

主要页面内容包括：

```text
┌─────────────────────────────────────────────┐
│       🌿 城市交通碳排放实时监测平台           │
│             当前时间 / 连接状态               │
├─────────────────────────────────────────────┤
│                                             │
│   路口实时状态卡片       │    实时告警        │
│                                             │
├─────────────────────────────────────────────┤
│                                             │
│              碳排放趋势图                    │
│                                             │
├──────────────────────┬──────────────────────┤
│     路口排放排行      │     小时排放分析      │
│                      │                      │
└──────────────────────┴──────────────────────┘
```

前端通过：

- REST API 获取初始数据
- WebSocket 获取实时数据

从而实现“**初始化查询 + 实时推送**”的组合模式。

---

## 📊 数据模型

当前核心数据模型包括三个主要业务实体：

### Intersection

城市交通路口基础信息。

```text
Intersection
├── id
├── intersectionId
├── name
├── district
├── lanes
├── hasSignalLight
├── locationLat
├── locationLng
└── description
```

### TrafficData

路口交通实时数据。

```text
TrafficData
├── id
├── intersectionId
├── vehicleCount
├── avgWaitTime
├── avgSpeed
├── truckRatio
├── carbonEmission
└── createTime
```

### OptimizationAdvice

交通优化建议。

```text
OptimizationAdvice
├── id
├── trafficDataId
├── intersectionId
├── level
├── title
├── content
├── status
└── createTime
```

建议状态：

```text
ACTIVE
   │
   ├── 管理人员处理
   │       ↓
   │    RESOLVED
   │
   └── 超时
        ↓
      EXPIRED
```

---

## 🔌 REST API

当前 API 主要分为四类。

### Traffic API

```text
GET /api/traffic/latest/{intersectionId}
GET /api/traffic/latest/all
GET /api/traffic/trend
GET /api/traffic/history
```

用于查询：

- 单路口最新数据
- 所有路口最新数据
- 交通趋势
- 历史交通数据

### Analysis API

```text
GET /api/analysis/ranking
GET /api/analysis/hourly
GET /api/analysis/congestion
GET /api/analysis/summary
```

用于：

- 排放排行
- 24 小时排放分布
- 拥堵统计
- 日汇总分析

### Advice API

```text
GET /api/advice/list
GET /api/advice/{id}
PUT /api/advice/{id}/resolve
GET /api/advice/statistics
```

用于：

- 查询优化建议
- 查看建议详情
- 标记建议已解决
- 查询建议统计

### System API

```text
GET  /api/system/intersection/list
POST /api/system/simulator/trigger
PUT  /api/system/simulator/interval
GET  /api/system/simulator/status
GET  /api/system/ws/online-count
```

用于：

- 查询路口
- 手动触发模拟数据
- 调整模拟频率
- 查看模拟器状态
- 查看 WebSocket 在线数量

---

## 🛠️ 技术栈

### Backend

| 技术 | 用途 |
|---|---|
| Java 17 | 开发语言 |
| Spring Boot 3.2.x | 应用框架 |
| Spring Data JPA | 数据访问 |
| Spring WebSocket | 实时通信 |
| Spring Scheduling | 定时任务 |
| Spring Event | 事件驱动 |
| Lombok | 简化 Java 代码 |
| Jackson | JSON 序列化 |
| Maven | 项目构建 |

### Database

| 技术 | 用途 |
|---|---|
| MySQL 8.x | 主要数据库 |
| H2 | 本地/演示场景 |

### Frontend

| 技术 | 用途 |
|---|---|
| HTML5 | 页面结构 |
| CSS3 | Dashboard 样式 |
| JavaScript | 前端逻辑 |
| ECharts 5 | 数据可视化 |
| WebSocket | 实时数据接收 |

---

## 🚀 快速开始

### 环境要求

建议准备：

```text
JDK >= 17
Maven >= 3.8
MySQL >= 8.0
Git
```

---

### 1. 克隆项目

```bash
git clone https://github.com/wangyulin/green-traffic.git

cd green-traffic
```

---

### 2. 创建数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE green_traffic
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

然后根据项目中的数据库脚本初始化表结构。

核心数据表：

```text
intersection
traffic_data
optimization_advice
```

---

### 3. 配置数据库

修改应用配置中的数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/green_traffic?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
```

请根据本地环境修改用户名和密码。

---

### 4. 编译项目

项目使用 Maven 多模块结构，可以在根目录执行：

```bash
mvn clean install
```

---

### 5. 启动应用

启动 API 应用模块中的 Spring Boot 启动类。

启动成功后，默认服务地址为：

```text
http://localhost:8080
```

WebSocket 地址：

```text
ws://localhost:8080/ws/traffic
```

---

### 6. 查看 Dashboard

Dashboard 静态资源位于：

```text
green-traffic-dashboard/
```

启动后访问：

```text
http://localhost:8080/
```

即可进入城市交通碳排放监控页面。

---

## 📖 Swagger API 文档

项目集成 OpenAPI / Swagger。

应用启动后，可以通过 Swagger UI 查看当前接口。

通常访问：

```text
http://localhost:8080/swagger-ui.html
```

或者：

```text
http://localhost:8080/swagger-ui/index.html
```

具体地址以当前应用配置为准。

---

## 📁 项目目录

```text
green-traffic/
│
├── green-traffic-api/              # REST API 与应用入口
├── green-traffic-common/           # 公共组件
├── green-traffic-core/             # 核心业务
├── green-traffic-dashboard/       # Web Dashboard
├── green-traffic-doc/              # 项目文档
├── green-traffic-infrastructure/  # 基础设施
├── green-traffic-model/            # 数据模型
├── green-traffic-push/             # WebSocket 推送
├── green-traffic-simulator/        # 交通数据模拟
│
├── sumo-work/                      # SUMO 仿真相关内容
├── 课程计划/                       # 项目学习/课程相关资料
├── pom.xml
└── README.md
```

---

## 🧩 架构设计

项目当前重点实践以下软件设计思想。

### 事件驱动

通过 Spring Event 将：

```text
数据生成
   ↓
业务处理
   ↓
实时推送
```

拆分为不同事件处理阶段。

核心事件包括：

```text
TrafficDataGeneratedEvent
TrafficDataProcessedEvent
```

这样可以避免 Simulator、Core、Push 三个模块之间形成强耦合调用。

---

### 规则引擎

通过：

```java
OptimizationRule
```

抽象交通优化规则。

新增规则时，可以采用：

```text
实现 OptimizationRule
        ↓
加入规则集合
        ↓
自动参与分析
```

避免修改核心业务流程。

---

### 分层与模块化

项目按照业务职责拆分模块，而不是将所有 Controller、Service、Repository 放在一个单体模块中。

主要体现：

```text
Common
  ↓
Model
  ↓
Core
 ↙  ↓  ↘
Simulator Push API
          ↓
      Dashboard
```

便于后续继续向领域化、六边形架构等方向演进。

---

## 🔍 当前项目定位

需要特别说明：

> **GreenTraffic 当前更适合作为“智慧交通 / 绿色交通”的技术原型与教学实践项目，而不是直接用于生产环境的城市交通管理平台。**

目前交通数据主要通过 Simulator 模块生成，项目的重点在于验证：

```text
交通数据
    ↓
实时处理
    ↓
碳排放计算
    ↓
规则分析
    ↓
优化建议
    ↓
实时可视化
```

完整业务闭环。

因此，项目可以作为后续接入真实交通数据的基础框架。

---

## 🚧 后续演进方向

如果继续向生产级“城市交通低碳智能管理平台”发展，可以考虑以下方向。

### 数据接入

将当前 Simulator 替换或扩展为：

```text
摄像头
地磁
雷达
信号机
GPS
浮动车
ETC
互联网地图
     ↓
Kafka / MQTT
     ↓
GreenTraffic
```

---

### 实时计算

当前事件驱动模型可以进一步升级为：

```text
Kafka
  ↓
Flink / Spark Streaming
  ↓
实时交通状态计算
  ↓
碳排放实时计算
  ↓
异常检测
```

---

### 数据存储

根据实际数据规模进一步引入：

```text
MySQL
  │
  ├── 基础业务数据
  │
  ├── Redis
  │     └── 实时状态 / 缓存
  │
  └── 时序数据库
        └── 海量交通时序数据
```

---

### 智能优化

当前规则引擎可以进一步演进为：

```text
规则引擎
    ↓
机器学习预测
    ↓
交通状态预测
    ↓
信号配时优化
    ↓
强化学习
    ↓
多路口协同控制
```

最终形成：

```text
感知
 ↓
分析
 ↓
预测
 ↓
决策
 ↓
优化
 ↓
评估
```

的闭环。

---

## 🧪 SUMO 仿真

项目仓库中包含：

```text
sumo-work/
```

可用于进一步开展交通仿真相关工作。

后续可以将当前随机交通数据模拟逐步替换为 SUMO 等交通仿真环境产生的数据，从而建立：

```text
SUMO
 ↓
交通流仿真
 ↓
车辆运行数据
 ↓
GreenTraffic
 ↓
碳排放计算
 ↓
优化策略
 ↓
仿真效果评估
```

的完整实验链路。

---

## 📚 项目文档

仓库中已经包含较完整的设计资料，包括：

- 系统架构
- 模块设计
- 数据库设计
- 核心业务流程
- REST API
- WebSocket
- Dashboard
- 异常处理
- 模拟器设计

详细设计建议逐步从 README 拆分到 `green-traffic-doc`，README 主要承担项目介绍和快速启动入口。

---

## 🤝 Contribution

欢迎通过以下方式参与项目：

1. Fork 项目
2. 创建 Feature 分支
3. 提交代码
4. 创建 Pull Request

建议提交前完成：

```bash
mvn clean test
```

并确保新增功能具有必要的测试和文档。

---

## 📄 License

当前仓库请以实际存在的 License 文件及仓库声明为准。

---

## 🌱 Project Vision

GreenTraffic 希望探索一个简单的问题：

> **如何把“交通拥堵”转化为可以量化、监测、分析和优化的“交通碳排放问题”？**

从实时交通数据出发：

```text
        交通感知
           ↓
        实时数据
           ↓
      ┌────┴────┐
      ↓         ↓
   拥堵分析   碳排放计算
      ↓         ↓
      └────┬────┘
           ↓
       优化建议
           ↓
       实时决策
           ↓
       低碳交通
```

让交通管理从“看到拥堵之后再处理”，逐步走向：

**实时感知 → 量化评估 → 主动优化 → 持续验证。**

---

**GreenTraffic · Green Mobility · Low Carbon City 🌿**