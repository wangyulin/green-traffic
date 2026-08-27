# Messaging / Push 迁移与未完成项清单

目的：总结当前仓库中关于 Push/Event/Messaging 迁移状态，列出剩余可执行任务与验证命令，便于分配和并行处理。

概述
- 能力端口：`MessagePublisher` / `MessageSubscriber` 已在 `green-traffic-core` 定义。
- Adapter：`RocketMQMessagePublisher` / `RocketMQMessageSubscriber`、`SpringEventsMessagePublisher` / `SpringEventsMessageSubscriber` 已在 `green-traffic-infrastructure` 实现。
- 装配：`green-traffic-bootstrap` 的 `BootstrapConfiguration` 使用 `@ConditionalOnMissingBean` 装配能力端口。

剩余待办（高优先级）
1. Schema 与契约确认
   - 定义 `Message<T>` 的版本与兼容策略（fields、messageType、metadata）。
2. 幂等与去重
   - 确认 producer/consumer 幂等关键字段，并在 `RocketMQMessagePublisher` / subscriber 中实现幂等策略或提供示例中间件（Redis、DB）。
3. DLQ / 重试策略
   - 在 `RocketMQMessageSubscriber` 与 `RocketMQMessagePublisher` 中补充可配置的重试与 DLQ 策略（配置项：`messaging.retry.*`，`messaging.dlq.enabled`）。
4. Trace / observability
   - 为消息 publish/consume 添加 tracing/log correlation（messageId -> traceId），并在日志中统一字段名。
5. Profile 与运行时 wiring 文档
   - 在 `green-traffic-doc` 中补充 `application-*.yml` 说明，明确 `messaging.type: rocketmq|springEvents` 的行为及 profile 使用。

小任务（中长期）
- 抽出 messaging adapter 子模块（可选），便于独立部署与复用。
- 增加契约测试（contract tests）用于验证 publisher/subscriber 与外部 MQ 的兼容性。

验证命令
```
mvn -DskipTests=false -pl green-traffic-core,green-traffic-infrastructure -am test
mvn -DskipTests=false -pl green-traffic-infrastructure test
```

如何贡献
1. 先从创建 Issue（使用仓库 Issue 模板）并在 Issue 中声明你要负责的子任务。2. 提交小 PR，修改文档/添加配置/实现幂等或 DLQ。3. 在 PR 描述中引用相关 contract tests 的运行结果。

参考文件
- `green-traffic-bootstrap/src/main/java/com/greentraffic/bootstrap/config/BootstrapConfiguration.java`
- `green-traffic-core/src/main/java/com/greentraffic/core/port/output/messaging/MessagePublisher.java`
- `green-traffic-infrastructure/src/main/java/com/greentraffic/infrastructure/messaging/rocketmq`


2026-08-27
