# 开发进度与迭代路线（DEV_ROADMAP）

说明：基于产品文档与模块化设计，列出建议的开发顺序、每步关键产出、依赖与验收准则，便于立项、排期与验收。

一、总体原则
- 优先搭建可运行的数据链路（Simulator → Ingest → Influx），再实现核心计算与推送；元数据与管理功能并行推进。
- 目标分为若干迭代（每迭代可为 1 周左右，视团队规模调整）。

二、开发顺序（建议）

1) 准备与基础设施（必先）
- 产出：仓库脚手架、CI 配置、开发环境文档、MySQL 实例与 InfluxDB 实例配置（含 retention policy）
- 依赖：无
- 验收：能在本地或测试环境运行数据库实例；DDL 能成功执行；Influx 可接收示例点

2) 数据模型与建表（MySQL） + Influx 测量定义
- 产出：执行 `mysql_ddls.sql`（创建表：intersections、latest_traffic_per_intersection、traffic_agg_*、optimization_advice、events_alerts 等），Influx measurement 规范与 line-protocol 示例
- 依赖：步骤1
- 验收：MySQL 表创建、Influx 写入/查询示例通过

3) Simulator + Ingest（首个端到端链路）
- 产出：`green-traffic-simulator`（可配置速率）；`/api/ingest/traffic` 接收端；Ingest 将清洗后数据写入 Influx 或转发事件
- 关键点：去重（intersectionId+timestamp）、时间容差、批量写入策略
- 验收：模拟器生成样本，Ingest 接收并在 Influx 中可查询到数据；端到端延迟目标 <2s

4) Core（计算 + 规则引擎 + 聚合写入 MySQL）
- 产出：`CarbonEmissionCalculator`、`TrafficDataProcessor`、规则实现（LongWait/LowSpeed/HighTruck 等）、写入 optimization_advice、更新 latest_traffic_per_intersection 与 traffic_agg_hourly 任务
- 关键点：Influx 与 MySQL 写入的一致性与幂等；规则防抖以避免重复告警
- 验收：对示例输入产生正确排放值与 Advice，发布 `TrafficDataProcessedEvent`

5) Push（实时推送）
- 产出：WebSocket 服务 `/ws/traffic`、订阅管理、消息封装（TRAFFIC_DATA / OPTIMIZATION_ADVICE / ALERT）
- 关键点：连接管理、心跳、鉴权、消息可靠性与重试
- 验收：WebSocket 客户端能接收并正确解析推送消息

6) API 层（查询、控制与管理）
- 产出：REST 接口（/api/traffic/latest、/api/traffic/trend、/api/analysis/ranking、/api/advice/{id}/resolve、模拟器控制等）及必要权限校验
- 关键点：跨存储聚合（Influx 快速查询 + MySQL joins）、接口性能优化（大屏接口）
- 验收：接口返回正确数据，模拟器可被远程控制，权限校验可用

7) Dashboard（前端大屏集成）
- 产出：大屏 demo，展示卡片、热力图、趋势图、告警与建议处理交互
- 依赖：步骤3-6
- 验收：能实时展示数据并处理/确认建议

8) 运维/管理与安全（并行推进）
- 产出：用户/角色管理、api_keys、data_sources 管理、ingest 日志、审计、Prometheus/Grafana 指标面板、备份/归档策略
- 验收：权限控制生效、审计日志可查、关键监控指标可视化

9) 可选：模型/预测/仿真扩展（后期）
- 产出：模型元信息、预测写入（Influx 或 MySQL）、预测评估任务
- 验收：批量预测可运行并能与真实值比对

三、迭代示例（短期 roadmap，三迭代）
- 迭代 1（1 周）：步骤1+2+3（准备环境 + 建表 + 搭通 Simulator → Ingest → Influx）
- 迭代 2（1 周）：步骤4（Core 完成基础计算与持久化聚合），并发布 ProcessedEvent
- 迭代 3（1 周）：步骤5+6（Push 实时推送 + API）和最小 Dashboard Demo

四、每步的风险与缓解
- 风险：Influx ↔ MySQL 同步不一致 → 缓解：幂等写入、补偿任务、写失败重试与 DLQ
- 风险：告警风暴 → 缓解：AlertDedupTask、规则抑制窗口、阈值冷却
- 风险：WebSocket 高并发 → 缓解：压测、连接池限制、网关/负载均衡

五、交付物与验收清单
- 基础设施：可运行 MySQL、Influx 示例配置与 retention
- 数据：MySQL DDL、Influx 示例 line-protocol、sample 数据集
- 功能：Simulator、Ingest、Core（计算+规则）、Push（WS）、API（查询/控制）、最小 Dashboard
- 运维：监控面板、备份策略、审计日志

六、下一步
- 若需要，我可以将上述拆分为详细任务清单（子任务 + 预计人日 + 优先级 + 负责人占位），并写入 `green-traffic-doc/DEV_ROADMAP.md` 的扩展部分或新文件 `green-traffic-doc/DEV_TASKS.md`。

---

（文件生成于项目 workspace，供团队直接参考与迭代排期）
