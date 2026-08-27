安全清单：删除 `green-traffic-model` 模块前后的确认项

概述
- 目标：确认 `green-traffic-model` 模块可以安全移除并完成清理。
- 当前状态：仓库中不存在 `green-traffic-model` 模块目录（无需删除）。

检查项（已验证）
- [x] 根 POM 中未列出 `<module>green-traffic-model</module>`（已移除）。
- [x] 仓库代码模块编译并运行所有单元测试：`mvn -DskipTests=false test` → BUILD SUCCESS（见测试日志）。
- [x] 代码里无对 `green-traffic-model` 的编译时依赖（artifactId / module），仅文档/说明仍引用。可安全删除模块目录（已不存在）。

建议的后续清理操作
1. 文档清理：
   - 把文档中对 `green-traffic-model` 的引用改为“已移除/合并到 `green-traffic-core` 或 `green-traffic-common`”，或保留为历史注记。
   - 我已在若干文档中将 `MetricPoint` 标注为“已迁移为域对象 `TrafficMetric`”。
2. IDE 配置：
   - 可选：清理 `.idea` 中对已删除文件的残留引用（防止 IDE 报错）。
3. 提交与 PR：
   - 生成一个包含本次变更（文档标注、测试修复、Mock 修正）的提交并打开 PR，便于代码审阅。建议在 PR 描述中说明“已移除 green-traffic-model，域对象迁移到 core”。

我已完成的实际工作
- 扫描并更新仓库中文档中 `MetricPoint` / `SimulationMetricPoint` 的若干引用（标注为已迁移或替换为 `TrafficMetric`/`SimulationTrafficMetric` 示例）。
- 修复了 Core 与 Infrastructure 的测试（删除对已删除类型的引用、修正 Mockito 用法）。
- 运行并验证了全量 `mvn -DskipTests=false test`：BUILD SUCCESS。

是否需要我：
- A. 继续把仓库内所有文档中的 `green-traffic-model` 文本全部替换为“已移除/迁移说明”，或
- B. 只清理 `.idea` 中的残留引用并提交这些变更，或
- C. 现在创建并推送一个包含本次修改的 commit + PR？

请选择 A、B、C 或 “先别动”。
