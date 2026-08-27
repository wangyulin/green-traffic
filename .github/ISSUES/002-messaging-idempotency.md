---
title: '[messaging] 实现 Producer/Consumer 幂等与去重策略'
labels: migration, messaging, reliability
---

## 目标
为消息发送与消费实现幂等支持，定义 idempotency key、去重窗口与示例中间件（Redis / DB）实现。

## Checklist
- [ ] 在 Message 中标准化 `messageId` 字段
- [ ] 提供 Redis-based 去重示例
- [ ] 在 RocketMQ subscriber 中增加去重中间层
- [ ] 在文档中说明配置项与注意事项

## 验证
- 增加单元/集成测试覆盖，模拟重复消息发送与消费。
