# 模块化数据库与技术设计（基于项目产品文档）

说明：本文件基于 `PRODUCT_SPEC.md`、`DATA_FLOW_EXAMPLE.md` 与 `DATA_PROCESSING.md` 的需求，划分系统模块，列出每个模块需要的数据库表（MySQL）与时序数据测量（InfluxDB），提供核心 MySQL DDL 示例、InfluxDB line-protocol 示例、并详述每个模块的业务流程、关键业务点、所需 API、后台任务与消息事件。

总原则：仅使用 MySQL（关系型元数据、事务、聚合表与权限）和 InfluxDB（高吞吐时序写入与实时查询）。

目录：
- 模块概览
- 每模块：表清单（MySQL）、InfluxDB measurement、MySQL DDL（核心表）、业务流程与关键点、API/任务/消息/技术组件
- InfluxDB 采集与同步策略
- 关键后台任务与调度
- 监控、运维与安全建议

---

**模块概览**
- Simulator（green-traffic-simulator）：生成模拟流量事件并发布至 Core/Ingest
- Ingest（可为 Core 内一部分）：接收、校验、清洗、去重、写入 InfluxDB/发布事件
- Core（green-traffic-core）：计算碳排放、规则引擎、生成建议、持久化聚合结果到 MySQL、发布处理完成事件
- Push（green-traffic-push）：监听处理事件并通过 WebSocket 广播给前端
- API（green-traffic-api）：REST 查询/控制/管理接口，聚合 MySQL 与 InfluxDB 数据
- Dashboard（前端，大屏）：显示数据与告警（非后端模块，仅列出需要的后端接口）
- 运维/管理：用户/权限/设备维护/数据源管理（MySQL 存储）

每个模块下面我将列出：所需 MySQL 表（并标注是否提供 DDL）、InfluxDB measurement 名称、业务流程、关键业务点、必要 API、后台任务、消息事件与使用到的技术组件。

---

模块：Simulator
- MySQL 表：无必需表（可选：`simulation_runs` 在 MySQL 中保存 run 元信息）
- InfluxDB measurements：N/A（Simulator 将写入 InfluxDB 或调用 Ingest API）
- 推荐 MySQL 表（可选，DDL 在 Core 模块处提供）：`simulations`, `simulation_runs`（用于记录场景与执行历史）
- 业务流程与关键点：
  - 定时生成 `TrafficDataDTO`（字段见 DATA_PROCESSING）并通过 HTTP POST 到 Ingest 接口或直接写入 InfluxDB line protocol。
  - 支持动态配置（`interval-ms`、路口数量、峰值模式）。
  - 关键点：幂等/唯一 id、生成节奏与批量写入策略（批量写入 Influx 降低压力）。
- 必要 API：`POST /api/simulator/start|stop|config`（已在项目中）
- 后台任务：无（由 Simulator 本身定时器触发）
- 消息/事件：`TrafficDataGeneratedEvent`（内部事件总线）

---

模块：Ingest（接收与预处理）
- MySQL 表：`data_sources`, 可选 `ingest_logs`（若写入频繁建议 Influx）
- InfluxDB measurements：直接写入 `traffic_data`（measurement）或通过 Core 处理后写入
- MySQL DDL（data_sources, ingest_logs）：见下方“核心 MySQL DDL”
- 业务流程与关键点：
  - 接收上报（Simulator/外部设备），执行去重、校验、清洗、时间标准化。
  - 把清洗后的原始样本写入 InfluxDB 的 `traffic_data` measurement（高频写入点）。
  - 发布 `TrafficDataGeneratedEvent` 到内部事件总线供 Core 异步消费（或直接调用 Core API）。
  - 关键点：去重策略（intersectionId+timestamp）、时间容差校验、快速响应（避免阻塞写入）。
- 必要 API：`POST /api/ingest/traffic`（接收原始数据）
- 后台任务：记录 ingest 日志、轮询 DLQ、异常重试
- 消息/事件：`TrafficDataGeneratedEvent`（payload 包含 raw payload 引用或解包字段）

---

模块：Core（计算、规则与持久化）
- MySQL 表：
  - `sensors`（可选，若需要设备级管理）
  - `intersections`
  - `traffic_agg_hourly`, `traffic_agg_daily`, `latest_traffic_per_intersection`
  - `optimization_advice`
  - `events_alerts`
  - `models`（模型元信息）
  - `model_predictions`（可选，若低频可放 MySQL；高频建议放 Influx）
- InfluxDB measurements：
  - `traffic_data`（原始高频采样）
  - `signal_events`（信号相位日志，如高频）
  - `vehicle_events`（单车事件，高频）
  - `model_predictions`（若高频）
- 业务流程与关键点：
  1) 监听 `TrafficDataGeneratedEvent` 或从 Ingest 接口接收清洗后的样本
  2) 调用 `CarbonEmissionCalculator` 进行碳排放计算（公式见 DATA_FLOW_EXAMPLE）
  3) 执行规则引擎（多个 `OptimizationRule`）生成 `OptimizationAdvice`（可能为 0..N 条）
  4) 写入 InfluxDB 的 `traffic_data`（如果 Ingest 未写入）并将聚合/下采样结果写入 MySQL 的 `traffic_agg_hourly/ daily`，并更新 `latest_traffic_per_intersection`
  5) 若 `Advice.level == HIGH`，创建 `events_alerts` 并标记 `status = OPEN`
  6) 发布 `TrafficDataProcessedEvent(ProcessedResult)`（包含 VO 用于 Push）
  - 关键点：一致性（Influx 写入与 MySQL 写入的顺序/幂等）、延迟（目标 <2s）、规则优先级与防抖（避免重复告警）
- 必要 API（Core / 被 API 调用）：内部接口或事件触发为主，外暴露：
  - `POST /api/core/processTraffic`（可选，用于手动或批量触发）
  - `GET /internal/metrics`（处理延迟与队列长度）
- 后台任务：
  - Aggregation Task：周期性从 Influx 下采样到小时/日聚合并写入 MySQL
  - Alert Dedup Task：合并重复/连续触发的告警
  - Model Prediction Task：若模型实时推送，可周期性写入预测值到 Influx 或 MySQL
- 消息/事件：
  - 入：`TrafficDataGeneratedEvent`
  - 出：`TrafficDataProcessedEvent`（包含 trafficDataVO 与 advices）

---

模块：Push（实时推送）
- MySQL 表：`push_subscriptions`（管理 WebPush/Webhook/Socket 订阅）
- InfluxDB measurements：无必要性写入（主要消费事件）
- 业务流程与关键点：
  - 监听 `TrafficDataProcessedEvent`，把 `TrafficDataVO`、`AdviceVO` 封装成 WebSocket 消息广播到 `/ws/traffic`。
  - 当 Advice.level == HIGH，额外生成 `ALERT` 消息并对订阅者或运维团队推送（WebSocket/HTTP webhook/邮件）。
  - 关键点：推送可靠性（断线重连、ACK、重试）、心跳/PING 维护连接、权限校验（握手时 token）。
- 必要 API：WebSocket endpoint `/ws/traffic`；管理订阅 API `POST /api/push/subscribe`、`DELETE /api/push/subscribe/{id}`
- 后台任务：连接清理、消息重试队列
- 消息/事件：消费 `TrafficDataProcessedEvent` 并发送 `TRAFFIC_DATA`、`OPTIMIZATION_ADVICE`、`ALERT` 消息

---

模块：API（REST，外部访问）
- MySQL 表：依赖 Core 中的表（查询聚合表、events、models、simulations、users 等）
- InfluxDB measurements：API 可直接查询 InfluxDB（例如趋势查询）或从 MySQL 聚合表读取（低延迟视图）
- 主要端点（根据 PRODUCT_SPEC）：
  - GET /api/traffic/latest/{intersectionId}
  - GET /api/traffic/latest/all
  - GET /api/traffic/trend?intersectionId=&minutes=
  - GET /api/analysis/ranking?date=
  - POST /api/advice/{id}/resolve
  - POST /api/simulator/config
  - POST /api/simulator/start|stop
  - 管理类：CRUD for intersections, sensors, users, roles
- 业务流程与关键点：
  - 聚合跨存储：短期趋势直接查询 Influx（快速），跨表 joins/权限检查在 MySQL。
  - 对大屏接口优化：`/api/traffic/latest/all` 应返回按需字段的扁平结构，结合缓存或 `latest_traffic_per_intersection` 表。
  - 权限：敏感操作需鉴权（后续建议 OAuth2/JWT）
- 后台任务：API 层常驻缓存（Redis，可选）与请求限流

---

模块：运维与管理（管理 UI / Cron）
- MySQL 表：`users`, `roles`, `api_keys`, `assets_maintenance`, `data_sources`, `ingest_logs`（若不放 Influx）
- 业务流程与关键点：
  - 用户权限管理、设备/路口配置、数据源配置、维护记录
  - 关键点：审计日志（谁做了什么）、API key 的安全存储（只保存哈希）、维护记录的可追溯性

---

核心 MySQL DDL（示例，供直接部署）
说明：以下 DDL 面向 MySQL 8，使用 InnoDB 引擎，UTF8MB4。去掉过多索引以降低写入成本，只保留关键索引，具体可按负载优化。

-- intersections
CREATE TABLE IF NOT EXISTS intersections (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  intersection_code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128),
  district VARCHAR(64),
  lanes INT,
  has_signal_light TINYINT(1) DEFAULT 1,
  latitude DECIMAL(9,6),
  longitude DECIMAL(9,6),
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  SPATIAL INDEX(idx_geom) (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sensors
CREATE TABLE IF NOT EXISTS sensors (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sensor_code VARCHAR(64) NOT NULL UNIQUE,
  type VARCHAR(32),
  model VARCHAR(64),
  installation_date DATE,
  status VARCHAR(32),
  latitude DECIMAL(9,6),
  longitude DECIMAL(9,6),
  intersection_id BIGINT,
  lane VARCHAR(32),
  direction VARCHAR(32),
  meta JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX(idx_intersection) (intersection_id),
  FOREIGN KEY (intersection_id) REFERENCES intersections(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- latest single sample per intersection (物化最新记录，快速大屏响应)
CREATE TABLE IF NOT EXISTS latest_traffic_per_intersection (
  intersection_id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128),
  carbon_emission DECIMAL(10,3),
  vehicle_count INT,
  avg_speed FLOAT,
  avg_wait_time FLOAT,
  truck_ratio FLOAT,
  advice_count INT DEFAULT 0,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- traffic_agg_hourly
CREATE TABLE IF NOT EXISTS traffic_agg_hourly (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  intersection_id VARCHAR(64) NOT NULL,
  interval_start DATETIME NOT NULL,
  avg_speed FLOAT,
  sum_volume BIGINT,
  max_occupancy FLOAT,
  class_counts JSON,
  record_count INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY ux_inter_interval (intersection_id, interval_start),
  INDEX idx_interval (interval_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- traffic_agg_daily (类似 hourly)
CREATE TABLE IF NOT EXISTS traffic_agg_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  intersection_id VARCHAR(64) NOT NULL,
  interval_start DATE NOT NULL,
  avg_speed FLOAT,
  sum_volume BIGINT,
  max_occupancy FLOAT,
  class_counts JSON,
  record_count INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY ux_inter_day (intersection_id, interval_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- optimization_advice
CREATE TABLE IF NOT EXISTS optimization_advice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  traffic_data_ref VARCHAR(128),
  intersection_id VARCHAR(64),
  level VARCHAR(16),
  title VARCHAR(256),
  content TEXT,
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_by VARCHAR(64),
  resolved_at TIMESTAMP NULL,
  INDEX idx_inter_status (intersection_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- events_alerts
CREATE TABLE IF NOT EXISTS events_alerts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(64),
  intersection_id VARCHAR(64),
  start_ts DATETIME,
  end_ts DATETIME,
  severity VARCHAR(16),
  description TEXT,
  status VARCHAR(32) DEFAULT 'OPEN',
  ack_by VARCHAR(64),
  ack_ts TIMESTAMP NULL,
  meta JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- users, roles, api_keys（简化）
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) UNIQUE,
  permissions JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) UNIQUE,
  email VARCHAR(128),
  full_name VARCHAR(128),
  role_id BIGINT,
  password_hash VARCHAR(256),
  last_login TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  key_hash VARCHAR(256),
  scopes JSON,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- data_sources & ingest_logs
CREATE TABLE IF NOT EXISTS data_sources (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128),
  type VARCHAR(64),
  config JSON,
  last_seen TIMESTAMP NULL,
  status VARCHAR(32),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ingest_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_id BIGINT,
  received_at TIMESTAMP,
  records INT,
  status VARCHAR(32),
  error TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_source_time (source_id, received_at),
  FOREIGN KEY (source_id) REFERENCES data_sources(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- simulations & simulation_runs
CREATE TABLE IF NOT EXISTS simulations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128),
  network_version VARCHAR(64),
  description TEXT,
  config JSON,
  created_by VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS simulation_runs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  simulation_id BIGINT,
  run_id VARCHAR(128),
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  status VARCHAR(32),
  result_uri VARCHAR(512),
  metrics JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- models（元信息）
CREATE TABLE IF NOT EXISTS models (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128),
  version VARCHAR(64),
  artifact_path VARCHAR(512),
  trained_at TIMESTAMP NULL,
  metrics JSON,
  deployed TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- assets_maintenance
CREATE TABLE IF NOT EXISTS assets_maintenance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  asset_id BIGINT,
  asset_type VARCHAR(64),
  maintenance_date DATE,
  type VARCHAR(64),
  notes TEXT,
  performed_by VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- regions
CREATE TABLE IF NOT EXISTS regions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128),
  polygon JSON,
  parent_id BIGINT NULL,
  meta JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (parent_id) REFERENCES regions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

---

InfluxDB measurements 与示例（line protocol）

- measurement: traffic_data
  - tags（索引，低基数）：sensor_id, intersection_id, region, type
  - fields：vehicle_count (int), avg_wait_time (float), avg_speed (float), truck_ratio (float), carbon_emission (float), class_counts (string/json)
  - 示例 line protocol：
    traffic_data,sensor_id=S-01,intersection_id=INTERSECTION_01 region=zoneA vehicle_count=12i,avg_wait_time=45.3,avg_speed=18.2,truck_ratio=0.12,carbon_emission=0.344 1692384725000000000

- measurement: signal_events
  - tags：intersection_id,controller_id,phase_id
  - fields：duration (int),source
  - 示例：
    signal_events,intersection_id=INTERSECTION_01,controller_id=CTRL-1 phase_id=2i,duration=60i,source="controller" 1692384725000000000

- measurement: vehicle_events
  - tags：sensor_id,intersection_id,classification
  - fields：speed,length,axle_count,plate_hash

- measurement: model_predictions (可选，高频放 Influx)
  - tags：model_id,sensor_id,intersection_id
  - fields：pred_speed,pred_volume,confidence

InfluxDB 策略建议：
- 为 `traffic_data` 配置热数据 `retention policy`（例如 30 天），并通过 `task`/`continuous query` 下采样到小时/日聚合（保存 365 天或更久）;
- 将下采样结果通过 ETL（Telegraf 输出或自写 sync 任务）写入 MySQL 的 `traffic_agg_hourly`/`traffic_agg_daily` 和 `latest_traffic_per_intersection`。

---

关键后台任务（调度与实现建议）
- AggregationTask（周期：每分钟/每小时）: 从 Influx 查询过去窗口数据，计算 SUM/AVG/MAX 写入 MySQL 聚合表。
- CleanupTask（周期：每日）: 清理 MySQL 与 Influx 的过期数据（基于 retention policy）；归档超期数据到对象存储。
- AlertDedupTask（周期：几十秒）: 对短时间内重复的 HIGH 告警进行合并，避免报警风暴。
- IngestRetryTask（周期：连续/秒级）: 处理 DLQ 中未能写入 Influx 的样本并重试。
- ModelPredictionTask（可选，周期：分钟级）: 批量触发模型预测并写入 Influx 或 MySQL。

---

消息与事件总览（异步流）
- TrafficDataGeneratedEvent (由 Simulator/Ingest 发布)：payload 包含 cleaned sample 或 raw_payload_ref
- TrafficDataProcessedEvent (由 Core 发布)：payload 包含 `TrafficDataVO`、`AdviceVO[]`、`alerts[]`
- OptimizationAdviceEvent：当生成 Advice 时发布（可与 ProcessedEvent 合并）
- AlertEvent：当 Advice.level==HIGH 时发布供 Push 模块与外部告警系统消费

消息语义注意点：事件应包含 `traceId` 与 `timestamp` 便于链路追踪与去重；事件处理设计为幂等（idempotent）以避免重复写入

---

API / 消息摘要（以开发者视角）
- REST API（外部）:
  - GET /api/traffic/latest/{intersectionId}
  - GET /api/traffic/latest/all
  - GET /api/traffic/trend?intersectionId=&minutes=
  - GET /api/analysis/ranking?date=
  - POST /api/advice/{id}/resolve
  - POST /api/simulator/start | /stop | /config
  - CRUD 管理端点：/api/intersections, /api/sensors, /api/users
- WebSocket（实时推送）:
  - Endpoint: /ws/traffic
  - 消息类型：TRAFFIC_DATA、OPTIMIZATION_ADVICE、ALERT、HEARTBEAT
- 内部消息（事件总线或轻量消息队列）:
  - TrafficDataGeneratedEvent
  - TrafficDataProcessedEvent
  - AlertEvent

---

关键业务流程示例（端到端）
1) 实时流（大屏）:
  - Simulator 定期写入 Influx 或调用 Ingest API。
  - Ingest 清洗并写入 Influx，并发布 TrafficDataGeneratedEvent。
  - Core 监听事件，计算碳排放并根据规则生成 Advice；将 Advice 写入 MySQL，并把处理结果封装后发布 TrafficDataProcessedEvent。
  - Push 监听并通过 WebSocket 广播 TRAFFIC_DATA 与 OPTIMIZATION_ADVICE，若 HIGH 同时广播 ALERT。

2) 趋势查询（历史分析）:
  - API /api/traffic/trend 查询 Influx（短期）或 MySQL 聚合表（长期/跨表）并返回给前端。

3) 告警确认:
  - 运维在前端看到 Advice → 调用 POST /api/advice/{id}/resolve（写回 optimization_advice.status 与 resolved_by/resolved_at）→ Core/Push 更新前端与历史记录

关键业务点（Need careful design）
- 幂等与去重：所有入库与事件处理必须支持幂等（use unique keys or dedup logs）
- 延迟控制：事件从生成到推送的端到端延迟目标 <2s，重点优化计算与事件传递路径
- 告警去噪：连续触发的规则需合并/去重，避免告警风暴与重复写入
- 数据一致性：Influx 与 MySQL 的聚合同步应有明确窗口与事务补偿（写入失败重试策略）
- 隐私与合规：车牌/图片只保留哈希或引用，并限制访问

---

监控、运维与安全建议（简要）
- 监控指标：事件吞吐、处理延迟、Influx 写入失败率、MySQL 主从延迟（若有）、WebSocket 连接数、告警数
- 日志与审计：规则触发日志、告警流转日志、用户操作审计
- 安全：API 鉴权（JWT/OAuth2）、WebSocket 握手 Token 校验、API Key 哈希存储

---

下一步建议（可选交付物）
- 生成并提交：MySQL 完整建表脚本文件 `green-traffic-doc/mysql_ddls.sql`（含索引与约束）
- 生成：InfluxDB line-protocol 示例与 `retention policy`/`task` 示例文件 `green-traffic-doc/influx_examples.md`
- 生成：Mermaid ER 图 `green-traffic-doc/ER_DIAGRAM.mmd`

如果需要，我现在可以：
1) 生成并保存 `mysql_ddls.sql`（包含上文 DDL），
2) 生成 Influx 示例文件（包含 retention/task/line protocol），或
3) 生成 Mermaid ER 图。
