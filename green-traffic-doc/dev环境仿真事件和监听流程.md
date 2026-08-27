更新了待办事项列表

Read 

Read 


简短回答：在 dev 环境，Spring Events 的消息由 `SpringEventsMessageSubscriber.onMessage(...)` 接收；发送端由 `SpringEventsMessagePublisher.publish(...)` 发布。

> 迁移状态（Messaging）：消息发布/订阅已通过 `MessagePublisher`/`MessageSubscriber` 能力端口规范化；dev 使用 Spring Events adapter，vm 使用 RocketMQ adapter。未完成：幂等、DLQ/重试与契约测试，见 `MIGRATION_MESSAGING.md`。

- 接收：`SpringEventsMessageSubscriber.onMessage` — `SpringEventsMessageSubscriber.java:43-49`  
- 发送：`SpringEventsMessagePublisher.publish` — `SpringEventsMessagePublisher.java:24-26`

补充：我已将仿真模块改为直接通过 `MessagePublisher` 发布（符合六边形架构），因此如果你希望在 dev 环境继续使用 Spring Events 路径，确保 `messaging.type=events`（默认）并保留 `SpringEventsMessagePublisher`/`SpringEventsMessageSubscriber`。需要我为 dev/profile 加回对 `ApplicationEventPublisher` 的兼容适配吗？