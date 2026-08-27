name: Messaging / Push Migration
about: 用于跟踪 Push/Event/Messaging 边界迁移的子任务与检查点
title: '[migration][messaging] {短描述}'
labels: migration, messaging
assignees: ''

---

## 目标
请在此 Issue 中声明你将负责的子任务（schema / 幂等 / DLQ / trace / profiles / contract-tests）。参照 `MIGRATION_MESSAGING.md` 的待办列表。

## 说明
- 修改类型（选择）: 文档 / 实现 / 测试 / 配置
- 关联模块: `green-traffic-core` / `green-traffic-infrastructure` / `green-traffic-bootstrap` / `green-traffic-doc`

## Checklist
- [ ] 在 `MIGRATION_MESSAGING.md` 中补充或确认具体实现细节
- [ ] 添加或更新 `application-*.yml` 的 messaging 配置说明
- [ ] 实现或补充幂等策略（示例/中间件/说明）
- [ ] 实现或补充 DLQ/重试配置与示例
- [ ] 添加或更新 contract tests（publisher/subscriber）
- [ ] 运行模块测试并在 PR 中附上输出结果

## 验证步骤（在本地）
```
mvn -DskipTests=false -pl green-traffic-core,green-traffic-infrastructure -am test
mvn -DskipTests=false -pl green-traffic-infrastructure test
```

## 备注
在 PR 描述中请引用本 Issue，并列出变更点与影响范围。
