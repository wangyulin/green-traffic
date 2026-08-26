# Contract Cleanup Scan — 初步建议

目标：识别并收口系统中的重复/迁移中的契约定义，确保 `core` 的 `core.port.output` 为契约单一来源。

发现要点：

- `com.greentraffic.core.port.output.messaging.TrafficMessageTypes`：当前为权威消息类型契约，广泛被 `infrastructure`、`simulator` 等模块引用。
- `com.greentraffic.core.port.output.messaging.Message`：作为统一消息契约已移入 `core`，并被多个 Adapter 使用。
- `green-traffic-common`：当前 `src/main/java/com/greentraffic/common/messaging` 目录为空/未包含契约实现，说明 `common` 中的旧契约已被迁移或移除。
- `green-traffic-model` 包含与 `core` 相似的实体（例如 `SimulationTrafficMetric` 在 `model` 中也有定义），需要确认哪一处为主数据模型或迁移目标。

建议行动项：

1. 确认契约归属：
   - 将所有消息/metrics/模拟相关的 Port（`core.port.output`）确认为契约单一来源。
   - 在 `green-traffic-common` 中删除或标注为 `@Deprecated` 的旧契约类（如仍存在历史类），并在迁移期保持兼容适配器。

2. 处理重复模型：
   - 对比 `core` 与 `model` 中的相同数据类型（例如 `SimulationTrafficMetric`），选择单一来源（建议将业务契约放在 `core`，`model` 保留持久化实体）；或在 `core` 提供转换层将 `core` 的契约映射到 `model` 的实体。

3. 测试与 CI：
   - 在 `green-traffic-core` 增加契约兼容性测试（已添加序列化与 ArchUnit 测试），并在 CI 流水线中作为关键门控项运行。
   - 在 CI 中确保 `simulator` 默认禁用，避免运行时内存问题干扰契约测试。

4. 文档与迁移计划：
   - 更新 `green-traffic-doc/architecture/README_V0.md` 中的迁移步骤与兼容性策略（标注迁移时间窗口与替换导入清单）。
   - 逐模块提交迁移 PR：先把 `infrastructure`、`simulator` 的导入改为 `core`，确认无编译/运行异常，再删除 legacy 类。

后续我可以：
- 生成详细的重复类清单（按模块列出文件路径）并建议逐步替换 PR 内容；或
- 基于你选定的策略（保留 core 还是 model 作为契约），我可以自动修改 import 并运行一次增量构建验证。
