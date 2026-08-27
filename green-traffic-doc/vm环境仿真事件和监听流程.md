
流程概览（从发送到写入时序库）：

- 发送（Publisher）：仿真或业务调用 `MessagePublisher.publish(...)`，在 vm 环境由 `RocketMQMessagePublisher` 实现负责把统一 `Message` 发到 RocketMQ。参见 `RocketMQMessagePublisher.publish` — `RocketMQMessagePublisher.java`

- 接收（MQ Listener）：RocketMQ 消息由 RocketMQ 客户端自动路由到 `RocketMQTrafficMessageListener.onMessage`，该类把消息交给内部的 subscriber 调度。参见 `RocketMQTrafficMessageListener` — `RocketMQTrafficMessageListener.java`

- 订阅分发（Subscriber）：`RocketMQMessageSubscriber.dispatchMessage` 会把接收到的 `Message<?>` 标准化（如果 payload 不是 `TrafficMetric` 会用 ObjectMapper 转换），再按 `messageType`/topic 找到注册的处理器并调用。参见 `RocketMQMessageSubscriber`（`dispatchMessage` / `normalizeMetricPayload`） — `RocketMQMessageSubscriber.java`

- 消费者（业务消费）：`TrafficMetricMessageConsumer` 在启动时订阅了 `traffic.data` 与 `co2.emission`，其 `consume` 方法把 `Message` 的 payload 转为 `TrafficMetric` 后，调用输入端口 `WriteTrafficMetricUseCase`。参见 `TrafficMetricMessageConsumer` — `TrafficMetricMessageConsumer.java`

- 核心用例（端口→应用服务）：`WriteTrafficMetricUseCase` 的实现 `MetricApplicationService.write(...)` 将 `WriteTrafficMetricCommand` 转为 ~~MetricPoint~~（已迁移为域对象 `TrafficMetric`），并调用输出端口 `MetricWritePort`。参见 `MetricApplicationService` — `MetricApplicationService.java`

- 写入时序库（Adapter）：`MetricWritePort` 在 vm 配置下由 `VictoriaMetricAdapter` 实现，它把 ~~MetricPoint~~（已迁移为域对象 `TrafficMetric`）批量转换为 Influx line-protocol（或 VM 支持的格式），并通过 HTTP 写入 VictoriaMetrics（有异步批量/重试逻辑）。参见 `VictoriaMetricAdapter.write`（及 flush 实现） — `VictoriaMetricAdapter.java`

要点和调试建议：
- 若 payload 丢失或类型不对，先在 `RocketMQMessageSubscriber.normalizeMetricPayload` 打断点/日志，查看原始 `Message.payload`（JSON→Java 映射是否正确）。  
- 确保发送端构造的 `Message` 中 `payload` 是 `TrafficMetric`（或可被 Jackson 转换为 `TrafficMetric` 的结构），并且 `messageType` 正确（`co2.emission` / `traffic.data`）。  
- 若需我帮你：我可以加入临时日志/断点位置（在 `RocketMQMessageSubscriber` 的 `dispatchMessage` 和 `normalizeMetricPayload`），或写一个小的集成测试（Testcontainers：RocketMQ + VictoriaMetrics）来跑通端到端。你想先做哪项？