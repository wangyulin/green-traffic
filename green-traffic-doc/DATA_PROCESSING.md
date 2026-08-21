 # DATA_PROCESSING（已格式化）

 说明：数据采集后必须执行的处理步骤、计算逻辑、存储建议与展示规范。

 目录
 - 原始数据示例
 - 必要处理步骤（接收→清洗→计算→聚合→推送）
 - 计算逻辑与衍生指标
 - 存储、索引与归档建议
 - 可视化字段与刷新策略
 - 数据工程注意事项

 一、原始数据示例
 - JSON 示例：
 ```json
 {
  "intersectionId": "INTERSECTION_01",
  "timestamp": "2026-08-18T10:12:05Z",
  "vehicleCount": 12,
  "avgWaitTime": 45.3,
  "avgSpeed": 18.2,
  "truckRatio": 0.12,
  "additional": { "sensorId": "S-01" }
 }
 ```

 二、必须执行的处理步骤（要点）
 1. 接收与去重：基于 `intersectionId+timestamp` 或唯一 id 做幂等/去重。
 2. 校验：必填字段与范围检查（`vehicleCount>=0`，`0<=truckRatio<=1`）。
 3. 清洗：默认值填充、异常截断、时间标准化为 UTC。
 4. 插值/对齐（可选）：用于固定窗口聚合时的空值处理。
 5. 富化：关联路口元数据（名称/行政区/车道数）。
 6. 计算：在 Core 层统一执行碳排放计算与规则判断。
 7. 聚合与存储：高频写入 Influx，按窗口下采样写入 MySQL 聚合表。

 三、核心计算逻辑（示例）
 - waitMinutes = avgWaitTime / 60
 - baseEmission = vehicleCount × baseRate × waitMinutes
 - truckWeight = 1 + truckRatio × (TRUCK_FACTOR - 1)
 - effectiveSpeed = min(avgSpeed, 60)
 - speedCorrection = 1.2 - (effectiveSpeed / 60) × 0.5
 - totalEmission = baseEmission × truckWeight × speedCorrection (保留 3 位小数)

 四、衍生指标与聚合
 - 单样本排放、每车平均排放、单位时间排放率、累积排放
 - 窗口：1min/5min/15min/1h/24h，聚合函数：SUM/AVG/MAX/COUNT

 五、存储与索引建议
 - 高频样本写 Influx（measurement `traffic_data`）
 - MySQL 保留聚合表与物化最新记录（`latest_traffic_per_intersection`）以供大屏查询
 - 建议索引：`(intersection_id, create_time)`；设定 Influx retention policy 并下采样

 六、展示与可视化（大屏要点）
 - 路口卡片必显字段：`intersectionId,name,carbonEmission,vehicleCount,avgSpeed,avgWaitTime,adviceCount`
 - 热力图：按坐标或路网聚合的单位时间排放
 - 趋势图：时间序列 carbonEmission（支持按分钟聚合）

 七、数据工程注意事项
 - NTP 时钟同步；后向兼容的 JSON schema；监控到达率/延迟/丢失率；提供异常场景用于 E2E 测试

 八、下一步（可交付）
 - 如需，我可以生成 `FIELDS_SPEC.md`、Influx 下采样 task 示例与 MySQL 索引建议脚本。

