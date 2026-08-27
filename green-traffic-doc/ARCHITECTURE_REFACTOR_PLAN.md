目标：
将项目的包结构从当前以技术层（application/domain/port）为主的分层，逐步演进为以“业务能力（capability）”为单位的分区，同时保持六边形（Hexagonal）架构思想：Adapter → Core（Ports）→ Domain，Bootstrap 负责装配。

高层原则：
- 核心不引用任何 Adapter/Infrastructure 实现；只通过 Ports（按业务能力命名）调用外部能力。
- Adapter（API、Simulator、Persistence、Messaging 等）依赖 Core 的 Port 接口。Adapter 可在实现内部使用 Domain 类型，但不要在 Controller/公共接口暴露 Domain 类型。
- 包结构以业务能力为单位（例如：metrics、simulation、alert），每个能力内部再按照 Hexagonal 拆分：api/adapter、application、domain、port、adapter/impl（infra）。

示例包布局（以 `metrics` 能力为例）：

```
com.greentraffic.metrics
├── api.adapter.rest             (API adapter/Controller + DTO)
│   └── controller/ dto / mapper
├── application                  (UseCases / Command / Query / ApplicationService)
│   ├── command
│   └── query
├── domain                       (Domain model、Domain Service、Domain Event)
├── port                         (Input/Output Port 接口，按业务能力命名)
│   ├── input
│   └── output
└── adapter                      (Infrastructure adapters 实现 ports)
    ├── persistence
    └── messaging
```

对比当前项目（高层步骤）：
1. 评估并列出当前 `core` 下的 domain/application/port 类，并按业务能力分类（例如：metrics、simulation、push）。
2. 在代码库中为每个能力创建目标包结构目录（先创建目录和 README/包说明，实际移动类分多步完成）。
3. 为每次迁移准备小型 PR（每个 PR 改动 3-10 个文件）并运行测试。推荐顺序：metrics → simulation → push → other。
4. 在 Bootstrap 层提供兼容 wiring（临时 bridge），以免在迁移期间造成运行时故障。
5. 更新 CI：新增包结构检测脚本和流水线检查，阻止暴露 Domain 到 Controller 的回归。
6. 最终把 `green-traffic-model` 的内容按职责搬回 Core（domain）、API（dto）、Infrastructure（entity），然后删除 `green-traffic-model`。

迁移详细步骤（incremental）：

步骤 A — 列表与准备（一次性）
- 运行扫描脚本（脚本已加入 `scripts/check_package_layout.sh`），获取当前技术分层包清单（application / domain / port 等）。
- 为每个业务能力创建目标包空间：`com.greentraffic.metrics`, `com.greentraffic.simulation`, `com.greentraffic.push`。
- 在每个目标包新增 `../README.md` 说明包职责和迁移策略。

步骤 B — 小步移动（按能力）
针对 `metrics` 能力举例：
1. 在 `com.greentraffic.metrics.api.adapter.rest` 中新增 DTO（如果尚无），并把 `api` 层 Controller 改为使用这些 DTO（不要直接引用 `core.domain`）。
2. 在 `com.greentraffic.metrics.application` 中新增 UseCase（如果尚无），把现有 `core.application` 中的 metrics 用例迁移为 `metrics.application` 并在新位置保留原型的桥接实现，原路径保留一版 delegate 到新位置，便于逐步切换。
3. 把 `TrafficMetric` 领域模型迁移到 `com.greentraffic.metrics.domain`（或在初期仅复制并保持兼容），并添加映射器：`adapter.persistence.mapper`。
4. 在 Adapter（`infrastructure`）新增或更新到 `com.greentraffic.metrics.adapter.persistence` 的实现，保证它实现 `com.greentraffic.metrics.port.output.MetricStore` 接口。
5. 提交 PR，运行 `mvn -pl green-traffic-core,green-traffic-api,green-traffic-infrastructure -am -DskipTests=false test` 验证。

步骤 C — 收尾
- 移除旧路径，删除桥接实现。
- 更新文档（`docs/ARCHITECTURE_REFACTOR_PLAN.md`、`../待整改问题.md`、`CONTRIBUTING.md`）。
- 在 CI 中强制检查：Controller/Adapter 公共 `src/main` 中不得出现 `import com.greentraffic.*.domain`（除 `adapter` 内部实现类被允许的包外）。

迁移命令示例（按能力，开发者在本地执行）：

```bash
# 在 IDE 中或使用 git mv 小步迁移单个类
git checkout -b refactor/metrics-package
# 用 git mv 或手动移动并修正 package 声明
git mv src/main/java/com/greentraffic/core/application/metrics/* src/main/java/com/greentraffic/metrics/application/
# 更新 package 声明
# 运行编译与测试
mvn -DskipTests=false -pl green-traffic-core,green-traffic-api,green-traffic-infrastructure -am test
```

CI 安全策略建议（迁移期间）
- 每个迁移 PR 必须包含：改动列表、受影响模块、是否新增桥接/兼容层。
- PR 合并前必须通过 `mvn -DskipTests=false test`，且 `scripts/check_package_layout.sh` 报告中只包含允许的过渡路径（在 PR 描述中注明）。
- 在所有迁移完成后，把 `scripts/check_package_layout.sh` 的告警升级为失败（即 CI 强制执行）。

回滚与兼容策略
- 迁移期间保留桥接实现（旧包的 facade/delegate），每个桥接类在 PR 中标注“deprecate→delete in X weeks”。
- 若某次迁移导致大量回归，可用 `git revert` 逐个 PR 回滚。

我已把一个检测脚本（`scripts/check_package_layout.sh`）和一个非阻塞的 GitHub Actions 检查工作流加入仓库，以便逐步执行迁移并得到可视化反馈（不会在初期阻塞合并）。

下一个动作建议（请选择其一）：
1) 我现在为 `metrics` 能力开始一次实际的小步迁移（移动 3 个核心类并提交 PR）；
2) 我把 CI 检查从“告警”升级为“阻塞（失败）”，适用于你已完成迁移并需要严格保证新分区；
3) 仅生成 PR 模板与 Issue 模板，便于分配给同事逐个能力执行迁移。

如果你选择 1，我会：
- 扫描并列出 metrics 相关的类清单；
- 在本仓库中创建 `com.greentraffic.metrics.*` 目标目录并把首批 3 个类移动（使用 `apply_patch`）；
- 运行 `mvn -pl green-traffic-core,green-traffic-api,green-traffic-infrastructure -am test` 并把结果反馈给你。

---
备注：迁移会触及大量 `package` 声明和 import 的修改，建议以小 PR 为单位逐步推进并充分利用 IDE 的批量重构功能以减少人为错误。