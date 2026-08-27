---
title: '[messaging] 配置 DLQ 与可配置重试策略'
labels: migration, messaging, reliability
---

## 目标
为订阅端与生产端补充可配置的重试与 DLQ（死信队列）策略，并在 adapter 中实现默认策略示例。

## Checklist
- [ ] 定义配置项（`messaging.retry.*`，`messaging.dlq.*`）
- [ ] 在 RocketMQ subscriber/publisher 中实现可配置的 retry + DLQ
- [ ] 更新 `MIGRATION_MESSAGING.md` 中的验证步骤

## 验证
- 在本地使用 contract test 模拟失败场景并验证消息流向 DLQ。