 # GreenTraffic 产品说明书（精简 & 目录化）

 说明：本文件为 GreenTraffic 的产品说明，已按主题结构化，便于开发与评审。

 目录
 - 概览
 - 范围与成功指标
 - 用户与关键用例
 - 总体架构
 - 模块职责
 - 功能需求与非功能需求
 - 数据模型摘要
 - 事件与异步流程
 - API 要点
 - 测试、部署与交付

 概览
 - 项目：GreenTraffic — 城市交通碳排放实时监测与优化
 - 目标：实时采集路口交通数据，计算碳排放，基于规则引擎生成优化建议并实时推送大屏与运维

 范围与成功指标
 - In-scope：模拟器、碳排放计算、规则引擎、WebSocket 推送、REST API、持久化、前端展示、基础运维
 - Out-of-scope：真实硬件接入（可后续接入适配层）、信号自动闭环控制
 - 成功指标：端到端延迟 <2s、可扩展到多路口、单节点 7×24 稳定运行、计算正确性

 用户与关键用例
 - 交管：大屏监控与高优先级告警处理
 - 分析：历史趋势/排行/导出
 - 运维：启动/停止模拟器、查看系统健康
 - 用例示例：实时监控、趋势查询、告警处理

 总体架构（摘要）
 - 层次：Dashboard ⇄ Push(WebSocket) ⇄ API ⇄ Core(计算/规则/持久化) ⇄ Simulator/Ingress ⇄ MySQL + InfluxDB
 - 关键事件：`TrafficDataGeneratedEvent` → `TrafficDataProcessedEvent`

 模块职责（精要）
 - Simulator：生成数据并发送到 Ingest
 - Ingest：校验/清洗/写入 Influx 或转发事件
 - Core：计算碳排放、执行规则、写 Advice/聚合到 MySQL、发布 ProcessedEvent
 - Push：监听事件并通过 WS 广播（TRAFFIC_DATA/ADVICE/ALERT）
 - API：对外查询/控制/管理接口

 功能与非功能要点
 - 采样：默认 5s，可配置；字段：intersectionId,timestamp,vehicleCount,avgWaitTime,avgSpeed,truckRatio
 - 计算：`CarbonEmissionCalculator`（统一在 core 实现，保留 3 位小数）
 - 规则：若干内建规则（基于 `OptimizationRule`）产生建议或告警
 - 性能：基线支持 6 路口每 5s；WebSocket 并发建议 1000

 数据模型摘要（见详细文档与 DDL）
 - 主要表：`intersections`, `traffic_agg_hourly`, `optimization_advice`, `events_alerts`, `latest_traffic_per_intersection`
 - 时序数据：写入 InfluxDB measurement `traffic_data`

 事件与异步流程（端到端）
 1. Simulator 生成并发送 `TrafficDataDTO`
 2. Ingest 清洗并写入 Influx，发布 `TrafficDataGeneratedEvent`
 3. Core 计算 → 规则 → 写 Advice/聚合 → 发布 `TrafficDataProcessedEvent`
 4. Push 广播 WS 消息，前端更新视图

 API 要点（示例）
 - GET /api/traffic/latest/{intersectionId}
 - GET /api/traffic/latest/all
 - GET /api/traffic/trend?intersectionId=&minutes=
 - GET /api/analysis/ranking?date=
 - POST /api/advice/{id}/resolve
 - POST /api/simulator/{start|stop|config}

 测试、部署与交付
 - 单元与集成测试覆盖计算、规则、事件流水线
 - 负载测试针对 WebSocket 并发与事件吞吐
 - 部署建议：容器化（Docker/K8s），MySQL + InfluxDB 为外部服务

 参考/扩展
 - 详细字段、示例、序列图请参见 `DATA_PROCESSING.md` 与 `DATA_FLOW_EXAMPLE.md`

---

（已精简并目录化，便于快速浏览。如需保留全文版原始说明，可保留备份。）
