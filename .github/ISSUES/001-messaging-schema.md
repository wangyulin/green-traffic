---
title: '[messaging] 定义 Message<T> 契约与版本'
labels: migration, messaging, spec
---

## 目标
定义统一的 `Message<T>` 格式与版本策略，明确字段、metadata、messageType、timestamp、messageId、schemaVersion 等。

## Scope
- 更新 `core` 中 `Message` 接口/类 Javadoc
- 增加契约示例 JSON（同仓库 `contract-tests` 可复用）
- 编写兼容性说明（如何处理旧版本）

## Checklist
- [ ] 定义字段与语义
- [ ] 编写示例 JSON
- [ ] 将规范加入 `green-traffic-doc`

## 验证
- 在 `green-traffic-infrastructure` 的 adapter 代码中对 `Message` 的序列化/反序列化进行契约测试。
