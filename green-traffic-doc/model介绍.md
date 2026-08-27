可以把整个 `GreenTraffic` 项目理解成一个**城市交通碳排放监控平台**。每个 Module 各司其职，最好不要互相“越界”。

## 一、整体架构

```text
                         ┌──────────────────────┐
                         │   green-traffic-api  │
                         │   Web API / 启动入口   │
                         └──────────┬───────────┘
                                    │
               ┌────────────────────┼────────────────────┐
               ▼                    ▼                    ▼
      ┌────────────────┐   ┌────────────────┐   ┌────────────────┐
      │    simulator   │   │      core      │   │      push      │
      │   传感器模拟    │   │     核心业务    │   │    实时推送     │
      └────────┬───────┘   └───────┬────────┘   └───────┬────────┘
               │                   │                    │
               └───────────────────┼────────────────────┘
                                   ▼
                         ┌────────────────────┐
                         │       model        │
                         │      数据模型       │
                         └─────────┬──────────┘
                                   ▼
                         ┌────────────────────┐
                         │       common       │
                         │      公共基础       │
                         └────────────────────┘
```

---

# 1. `green-traffic-common`

### 定位：**公共基础模块**

这是整个项目最底层的公共模块。

主要放一些**所有模块都可能使用的通用代码**。

目录：

```text
green-traffic-common
└── src/main/java/com/greentraffic/common/
    ├── result/
    ├── exception/
    └── utils/
```

### `result/`

负责统一接口返回结果。

例如：

```java
public class Result<T> {

    private Integer code;

    private String message;

    private T data;
}
```

Controller 最终可以统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

还可以放：

```text
Result
PageResult
ErrorCode
```

---

### `exception/`

负责统一异常体系。

例如：

```text
BusinessException
GlobalExceptionHandler
```

业务代码：

```java
if (vehicle == null) {
    throw new BusinessException("车辆不存在");
}
```

然后统一转换成 HTTP 返回结果。

---

### `utils/`

放通用工具：

```text
DateUtils
JsonUtils
StringUtils
BeanUtils
IdUtils
```

**原则：**

> common 不应该包含具体的交通业务逻辑。

例如：

```text
❌ CarbonEmissionCalculator
❌ TrafficVehicleService
❌ TrafficAlarmService
```

这些应该放到 `core`。

---

## 2. `green-traffic-model`（模块迁移说明）

注：`green-traffic-model` 模块已从代码库中移除。本仓库的领域模型现已按架构重构迁移到：

- `green-traffic-core`：核心业务相关的领域对象（如 `TrafficMetric`、`SimulationTrafficMetric`、应用服务与端口契约）；
- `green-traffic-common`：工具型或跨模块共享的 DTO/VO/通用类型（如通用响应、时间工具等）。

文档中原有的 `green-traffic-model` 示例保留为历史参考，但请以 `core`/`common` 中的实际类为准。

对应：

```java
public class TrafficQueryDTO {

    private String roadId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
```

---

## `vo/`

VO：

> 后端 → 前端

例如：

```text
TrafficOverviewVO
CarbonEmissionVO
TrafficAlarmVO
OptimizationSuggestionVO
```

例如：

```json
{
  "vehicleCount": 1250,
  "carbonEmission": 325.6,
  "congestionLevel": "HIGH"
}
```

---

## `enums/`

系统枚举。

例如：

```text
VehicleType
AlarmLevel
CongestionLevel
EmissionLevel
TrafficDirection
```

例如：

```java
public enum VehicleType {

    CAR,
    BUS,
    TRUCK,
    TAXI,
    NEW_ENERGY
}
```

### model 最重要的原则

> **model 尽量是“纯数据”，不要在里面写业务逻辑。**

也就是说：

```text
model
  ↓
定义数据是什么

core
  ↓
定义数据怎么计算、怎么处理
```

---

# 3. `green-traffic-simulator`

### 定位：**模拟交通传感器**

这个模块主要是为了你的项目在没有真实交通传感器的情况下，能够**自己产生交通数据**。

例如每 5 秒产生一次：

```text
时间：20:30:01

道路：长安街
车流量：1250
平均速度：38 km/h
小汽车：800
公交车：100
卡车：150
新能源车：200
```

目录：

```text
green-traffic-simulator
└── src/main/java/com/greentraffic/simulator/
    ├── TrafficSensorSimulator.java
    ├── config/
    └── generator/
```

---

## `TrafficSensorSimulator`

负责定时运行。

例如：

```java
@Scheduled(fixedDelay = 5000)
public void generateTrafficData() {

}
```

每 5 秒生成一次数据。

---

## `config/`

模拟参数。

例如：

```yaml
green-traffic:
  simulator:
    interval-ms: 5000
    vehicle-min: 100
    vehicle-max: 2000
```

---

## `generator/`

真正负责：

> “随机生成什么样的交通数据？”

例如：

```text
TrafficFlowGenerator
VehicleGenerator
SpeedGenerator
TrafficAlarmGenerator
```

可以模拟：

```text
正常交通
↓
车流增加
↓
拥堵
↓
严重拥堵
↓
碳排放增加
```

### 这个模块的价值

以后你接入真实传感器以后：

```text
现在：

simulator → core

以后：

真实传感器 → 消息队列 → core
```

所以 simulator 实际上相当于一个**测试数据源**。

---

# 4. `green-traffic-core`

### 定位：**整个系统最核心的业务模块**

如果说：

```text
model = 数据是什么

simulator = 数据从哪里来

api = 数据怎么提供给别人

push = 数据怎么实时推给前端

core = 数据来了以后怎么处理
```

那么 `core` 就是整个系统的“大脑”。

目录：

```text
green-traffic-core
└── src/main/java/com/greentraffic/core/
    ├── service/
    ├── calculator/
    ├── optimizer/
    ├── repository/
    └── event/
```

---

## `service/`

业务服务层。

例如：

```text
TrafficFlowService
CarbonEmissionService
TrafficAlarmService
OptimizationService
```

例如：

```java
public CarbonEmissionVO calculateEmission(
        TrafficFlow trafficFlow) {

    // ...
}
```

---

## `calculator/`

### 碳排放计算引擎

这是你这个项目非常有特色的地方。

例如：

```text
车辆数量
   ↓
车辆类型
   ↓
平均速度
   ↓
燃料类型
   ↓
排放因子
   ↓
CO₂排放量
```

可以设计：

```text
CarbonEmissionCalculator
```

甚至进一步做成策略：

```text
EmissionCalculator
       │
       ├── CarEmissionCalculator
       ├── BusEmissionCalculator
       ├── TruckEmissionCalculator
       └── NewEnergyEmissionCalculator
```

---

# 5. `optimizer`

### 定位：**交通优化建议规则引擎**

例如系统发现：

```text
道路 A
车流量：3500
平均速度：12km/h
拥堵指数：8.9
碳排放：很高
```

那么系统可以给出：

```text
建议：

1. 延长绿灯时间
2. 调整信号灯配时
3. 建议车辆绕行
4. 优先新能源车辆通行
```

例如：

```text
TrafficOptimizationEngine
        │
        ├── CongestionRule
        ├── EmissionRule
        ├── SignalTimingRule
        └── VehicleDiversionRule
```

这部分就是项目的**智能优化能力**。

---

# 6. `repository`

### 定位：**数据库访问层**

使用 Spring Data JPA。

例如：

```java
public interface TrafficFlowRepository
        extends JpaRepository<TrafficFlow, Long> {

}
```

负责：

```text
查询交通数据
保存交通数据
查询碳排放
查询告警
查询历史数据
```

典型关系：

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

注意：

> Repository 负责“数据怎么存取”，Service 负责“业务怎么处理”。

---

# 7. `event`

### 定位：**领域事件**

这个模块可以做成可选能力。

例如：

```text
收到新的交通数据
        ↓
TrafficDataReceivedEvent
        ↓
       ├── core 计算碳排放
       ├── 判断是否拥堵
       ├── 判断是否告警
       └── push 推送前端
```

这样后面系统扩展会比较容易。

例如：

```java
public class TrafficDataReceivedEvent {

    private TrafficFlow trafficFlow;
}
```

---

# 8. `green-traffic-push`

### 定位：**实时数据推送**

主要解决：

> 后端数据发生变化以后，怎么实时通知前端大屏？

目录：

```text
green-traffic-push
└── src/main/java/com/greentraffic/push/
    ├── websocket/
    └── listener/
```

---

## `websocket/`

负责 WebSocket。

例如：

```text
浏览器大屏
     ↑
     │ WebSocket
     │
green-traffic-push
     ↑
     │
    core
```

例如实时推送：

```json
{
  "type": "TRAFFIC_UPDATE",
  "roadId": "ROAD-001",
  "vehicleCount": 1520,
  "carbonEmission": 356.8
}
```

前端不需要不停地：

```text
每 1 秒请求一次 REST API
```

而是：

```text
后端数据变化
     ↓
WebSocket
     ↓
浏览器立即更新
```

---

# 9. `listener`

负责监听业务事件。

例如：

```text
TrafficDataReceivedEvent
          ↓
       Listener
          ↓
       WebSocket
          ↓
       Dashboard
```

也就是说：

```text
core
 ↓
产生事件
 ↓
push
 ↓
监听事件
 ↓
WebSocket
 ↓
前端大屏
```

这样 `core` 不需要直接依赖 WebSocket。

这是一个比较好的**解耦设计**。

---

# 10. `green-traffic-api`

### 定位：**系统对外入口 + Spring Boot 启动模块**

这是最终运行的模块。

目录：

```text
green-traffic-api
└── src/main/java/com/greentraffic/api/
    ├── controller/
    ├── config/
    ├── aop/
    └── GreenTrafficApplication.java
```

---

## `controller/`

负责 REST API。

例如：

```text
GET /api/traffic/realtime

GET /api/traffic/history

GET /api/emission/statistics

GET /api/alarm/list

GET /api/optimization/suggestions
```

Controller 不应该自己计算碳排放。

正确：

```text
Controller
    ↓
Service
    ↓
Calculator
    ↓
Repository
```

而不是：

```text
Controller
    ↓
自己计算一大堆业务
```

---

## `config/`

系统配置。

例如：

```text
WebMvcConfig
JacksonConfig
SwaggerConfig
WebSocketConfig
CorsConfig
```

---

## `aop/`

AOP。

例如做：

```text
接口访问日志
请求耗时
操作日志
异常日志
```

可以记录：

```text
POST /api/traffic/query
耗时：23ms
用户：xxx
结果：success
```

---

# 11. 最后用一个真实业务流程串起来

例如：

> “某个路口突然出现大量车辆，系统计算碳排放并实时显示在大屏上。”

整个流程就是：

```text
                 ① 模拟交通数据
                        │
                        ▼
              ┌─────────────────┐
              │    simulator    │
              │ 产生车辆/速度数据 │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │      model      │
              │ TrafficFlow     │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │      core       │
              │                 │
              │ ① 接收交通数据   │
              │ ② 计算碳排放     │
              │ ③ 判断拥堵       │
              │ ④ 判断告警       │
              │ ⑤ 生成优化建议   │
              └───────┬─────────┘
                      │
             ┌────────┴─────────┐
             ▼                  ▼
      ┌─────────────┐    ┌─────────────┐
      │    push     │    │  repository │
      │ WebSocket   │    │   数据库     │
      └──────┬──────┘    └─────────────┘
             │
             ▼
      ┌──────────────┐
      │  Dashboard   │
      │ 实时交通大屏  │
      └──────────────┘
```

与此同时，用户通过 REST API 查询：

```text
Dashboard
    │
    │ HTTP
    ▼
  api
    │
    ▼
  core
    │
    ├── calculator
    ├── optimizer
    └── repository
```

---

## 最简单的记忆方式

你可以把这 6 个模块记成：

| 模块            | 一句话                  |
| ------------- | -------------------- |
| **common**    | **大家都能用的工具**         |
| **model**     | **系统里的数据长什么样**       |
| **simulator** | **负责制造交通数据**         |
| **core**      | **负责处理数据，是系统大脑**     |
| **push**      | **负责把结果实时推给前端**      |
| **api**       | **负责对外提供接口，并启动整个系统** |

最终就是：

> **simulator 负责“产生数据”，model 负责“定义数据”，core 负责“处理数据”，repository 负责“保存数据”，push 负责“实时发送数据”，api 负责“对外提供服务并启动系统”，common 负责“提供公共基础能力”。**
