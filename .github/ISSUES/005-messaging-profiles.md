---
title: '[messaging] 明确 profiles 与 runtime wiring 文档（dev/vm/prod）'
labels: migration, messaging, docs
---

## 目标
补充 README/文档，说明 `messaging.type` 在不同 profile 下的行为、示例 `application-*.yml` 配置以及如何切换 adapter。

## Checklist
- [ ] 补充 `green-traffic-doc` 中的 profile 说明
- [ ] 在 `green-traffic-bootstrap` 中列出支持的 `messaging.type` 候选值
- [ ] 添加验证命令与示例启动脚本

## 验证
- 使用不同 profile 本地启动并验证 publisher/subscriber 行为（dev -> Spring Events，vm -> RocketMQ）。