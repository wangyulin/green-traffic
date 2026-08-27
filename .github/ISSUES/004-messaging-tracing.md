---
title: '[messaging] 添加消息 Trace/Observability (messageId->traceId correlation)'
labels: migration, messaging, observability
---

## 目标
为消息发布/消费增加 traceId/log correlation 支持，便于链路追踪与问题排查。

## Checklist
- [ ] 在 Message metadata 中标准化 `traceId` 字段
- [ ] 在 publisher/subscriber 处把 traceId 注入日志 MDC
- [ ] 在文档中提供部署/配置示例

## 验证
- 运行集成测试并在日志中验证 messageId -> traceId 链路。