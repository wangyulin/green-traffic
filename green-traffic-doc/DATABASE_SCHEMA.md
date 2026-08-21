# 数据库表设计（建议）

本文档汇总了基于产品介绍的完整数据库表建议：字段要点、字段说明、主键/索引/外键、设计原因与注意事项，便于用于 API、仿真、模型、推送与可视化需求。

## 总览
- sensors: 传感器/检测设备元数据
- intersections: 路口/交叉口/节点信息
- traffic_data: 时序原始测量（主时序表）
- traffic_agg_hourly / traffic_agg_daily: 聚合表（按小时/日）
- vehicle_events: 单车检测（可选，车辆层面数据）
- signal_controllers / signal_plans: 信号控制与方案
- signal_events: 信号运行日志/相位记录
- simulations / simulation_runs: 仿真场景与运行记录
- models / model_predictions: 模型元数据与预测输出
- events_alerts: 异常/拥堵/事故/告警记录
- users / roles / api_keys: 用户与认证/授权数据
- push_subscriptions: 推送/订阅管理
- data_sources / ingest_logs: 数据接入与采集监控
- assets_maintenance: 设备维护记录（可选）
- regions / geo_zones: 区域/行政分区/自定义围栏

---

## 表详细设计（含字段说明）

1) sensors — 传感器/设备元数据
- 关键字段（含字段说明）：
  - id (PK, bigint): 主键，唯一标识传感器记录
  - sensor_code (varchar, UNIQUE): 设备编码/序列号，外部系统的唯一标识
  - type (varchar): 传感器类型（例：`loop`/`camera`/`rsu`）
  - model (varchar): 厂商/型号信息
  - installation_date (date): 安装日期
  - status (varchar): 状态（`online`/`offline`/`maintenance`）
  - latitude (decimal): 纬度
  - longitude (decimal): 经度
  - intersection_id (FK -> intersections.id): 所属路口外键（可为空）
  - lane (varchar): 设备所在车道（可选）
  - direction (varchar): 朝向或行驶方向描述（可选）
  - meta (jsonb): 扩展属性（固件版本、安装角度、校准参数等）
  - created_at (timestamp): 记录创建时间
  - updated_at (timestamp): 记录更新时间
- 索引：sensor_code、空间索引（latitude/longitude）
- 推荐存储：MySQL（关系型元数据，低写入频率，方便事务与关联查询）

2) intersections — 路口/节点信息
- 字段（含说明）：
  - id (PK): 主键，路口唯一标识
  - name (varchar): 路口名称
  - node_code (varchar): 路口编码/外部标识
  - latitude (decimal): 路口中心点纬度
  - longitude (decimal): 路口中心点经度
  - geometry (geometry/geojson): 路口几何（点/多边形）用于空间查询
  - region_id (FK -> regions.id): 所属区域外键
  - meta (jsonb): 额外属性（车道路数、速度限制等）
  - created_at (timestamp): 创建时间
- 推荐存储：MySQL（地理元数据；MySQL 支持空间索引，可用于地图查询）

3) traffic_data — 时序原始测量（主时序表）
- 字段要点（含说明）：
  - id (bigserial PK): 自增主键
  - sensor_id (FK -> sensors.id): 上报数据的传感器
  - ts (timestamp with time zone, UTC): 观测时间（UTC）
  - speed (float): 平均车速（需在系统中约定单位）
  - volume (int): 车辆流量/计数（该时刻或窗口内通过车辆数）
  - occupancy (float): 占有率（百分比或比例）
  - avg_headway (float): 平均车头时距（秒）
  - class_counts (jsonb): 各车类计数（如{"car":10,"truck":2}）
  - raw_payload (jsonb/blob): 原始上报数据（厂商格式、原始消息）
  - quality_flag (smallint): 数据质量标志（0=正常,1=异常,2=缺失等）
  - created_at (timestamp): 写入时间
- 主键/唯一性：建议在业务层或 DB 层保证 (sensor_id, ts) 的唯一性以防重复写入
- 分区策略：按日或按月分区以支持高写入与归档
- 索引：(sensor_id, ts)、单列 ts 索引
- 推荐存储：InfluxDB（高吞吐时序数据写入与查询优化），在 InfluxDB 中作为 measurement `traffic_data` 存储；在 MySQL 中仅保留汇总/聚合视图以便 joins

4) traffic_agg_hourly / traffic_agg_daily — 聚合表（按小时/按日）
- 字段（含说明）：
  - id (PK): 主键
  - sensor_id (FK) / intersection_id (FK): 聚合维度（按传感器或路口）
  - interval_start (timestamp): 聚合区间起始时间（例如小时或日的0点）
  - avg_speed (float): 区间内平均速度
  - sum_volume (bigint): 区间总流量
  - max_occupancy (float): 区间最大占有率
  - class_counts (jsonb): 区间车辆分类计数
  - record_count (int): 用于计算聚合的原始记录数量
  - created_at (timestamp): 记录生成时间
- 用途：为 Dashboard、报表与快速查询提供预聚合数据，降低对原始表的扫描压力
- 推荐存储：MySQL（作为 Dashboard/报表的主表，便于跨表 joins）；也可从 InfluxDB 下采样并同步聚合结果到本表

5) vehicle_events — 单车检测（可选，车辆级别）
- 字段（含说明）：
  - id (PK): 主键
  - sensor_id (FK -> sensors.id): 检测点
  - detected_at (timestamp): 检测时间
  - speed (float): 检测到的车辆速度
  - length (float): 车辆长度（米）
  - axle_count (int): 轴数
  - classification (varchar): 车辆类别标签
  - plate_hash (varchar): 车牌哈希/脱敏值（出于隐私，不存明文）
  - raw_image_ref (varchar): 图片/视频在对象存储中的引用
  - created_at (timestamp): 写入时间
