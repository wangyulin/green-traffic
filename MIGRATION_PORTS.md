GreenTraffic 端口迁移说明

目的：将技术导向的旧端口（MetricWritePort / MetricQueryPort / SimulationMetricWritePort）替换为按业务能力命名的能力端口（TrafficMetricStore / SimulationMetricStore），并统一仓库中的文档与测试引用。

替换映射：
- MetricWritePort -> TrafficMetricStore
- MetricQueryPort -> TrafficMetricStore (查询能力合并到同一能力端口)
- SimulationMetricWritePort -> SimulationMetricStore

操作建议（按小步替换）：
1. 优先修改单元测试中对旧端口的引用（mock/import/assert），确保改为能力端口类型。
2. 修改 bootstrap 装配为注入能力端口（已移除旧后备 bean 工厂）。
3. 修改 ApplicationService wiring 为能力端口（已完成在部分文件）。
4. 逐步在文档中标注或替换旧端口术语，并在 PR 中附上测试命令。

本地验证命令：
```bash
mvn -DskipTests=false -pl green-traffic-core,green-traffic-infrastructure -am test
```

回退策略：若删除旧 bean 导致构建失败，可在短期内临时在 `BootstrapConfiguration` 中重新添加旧 bean 工厂（带 `@Deprecated` 注释）直至所有引用替换完毕。

贡献者模板要点（PR 需包含）：
- 受影响文件列表
- 修改点说明（代码 + 测试 + 文档）
- 验证命令和本地输出截屏或日志片段
- 若涉及 bootstrap 变更，说明如何运行本地 Spring 上下文进行验证
