 # DATA_FLOW_EXAMPLE（示例化、便于阅读）

 说明：提供两组端到端示例（正常/拥堵），展示从采集到计算、持久化与推送的完整流程和消息格式。

 假定常量
 - CAR_IDLE_RATE = 0.0307 kg/(vehicle·min)
 - TRUCK_FACTOR = 2.5
 - 速度上限 60 km/h；保留 3 位小数

 示例 A：正常流量（不触发告警）
 1) 原始数据：
 ```json
 { "intersectionId":"INTERSECTION_01","timestamp":"2026-08-18T10:12:05Z","vehicleCount":12,"avgWaitTime":45.3,"avgSpeed":18.2,"truckRatio":0.12 }
 ```
 2) 处理要点：校验必填/范围，去重，时间标准化
 3) 计算（步骤）：
 - waitMinutes = 45.3 / 60
 - baseEmission = 12 × CAR_IDLE_RATE × waitMinutes
 - truckWeight = 1 + 0.12 × (TRUCK_FACTOR - 1)
 - effectiveSpeed = min(18.2,60)
 - speedCorrection = 1.2 - (effectiveSpeed/60)*0.5
 - totalEmission ≈ 0.344 kg（保留 3 位小数）
 4) 写入/事件/推送：写入 Influx（traffic_data）或 DB，再发布 `TrafficDataProcessedEvent` 并通过 WS 推送 `TRAFFIC_DATA`。

 示例 B：拥堵触发 HIGH 建议
 1) 原始数据：
 ```json
 { "intersectionId":"INTERSECTION_05","timestamp":"2026-08-18T17:05:10Z","vehicleCount":30,"avgWaitTime":75.0,"avgSpeed":5.0,"truckRatio":0.20 }
 ```
 2) 计算结果约为 1.733 kg；LongWaitTimeRule（阈值 60s）触发，生成 `OptimizationAdvice(level=HIGH)`。
 3) 写入：traffic_data、optimization_advice → 发布 `TrafficDataProcessedEvent` → Push 广播 `TRAFFIC_DATA`、`OPTIMIZATION_ADVICE`、`ALERT`。

 序列图（流程）
 ```mermaid
 sequenceDiagram
   Simulator->>Ingest: POST raw JSON
   Ingest->>Ingest: 校验 / 清洗 / 去重
   Ingest->>Core: publish TrafficDataGeneratedEvent
   Core->>Core: 计算 carbonEmission
   Core->>Core: 规则评估 -> 生成 Advice
   Core->>DB: INSERT aggregation / advice
   Core->>Push: publish TrafficDataProcessedEvent
   Push->>Frontend: WebSocket broadcast (TRAFFIC_DATA / ADVICE / ALERT)
 ```

 字段与质量控制要点
 - 去重：intersectionId + timestamp
 - 时间容差：超出 ±5 分钟标注异常
 - 缺失值：关键字段入 DLQ，非关键字段用默认值
 - 单元测试：可复用样例 A/B 验证 `CarbonEmissionCalculator` 与规则

