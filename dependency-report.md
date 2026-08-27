# GreenTraffic 模块依赖报告

<!-- 注：`green-traffic-model` 模块已移除；文中对该模块的历史引用保留为说明，实际模型已迁移到 `green-traffic-core` 或 `green-traffic-common` -->

生成时间: 2026-08-26

## 模块列表（默认构建）
- green-traffic-common
- green-traffic-model
- green-traffic-core
- green-traffic-push
- green-traffic-api
- green-traffic-infrastructure

> 注意: `green-traffic-simulator` 已移入 `with-simulator` profile，默认不参与构建。

## 直接模块依赖（来自各模块 pom.xml）

- green-traffic-api
  - green-traffic-common
  - green-traffic-model
  - green-traffic-simulator (optional)
  - green-traffic-core
  - green-traffic-push
  - green-traffic-infrastructure

- green-traffic-core
  - (已移除对 green-traffic-common 的运行时依赖)
  - spring-boot-starter
  - mybatis-spring-boot-starter

- green-traffic-infrastructure
  - green-traffic-common (optional)
  - green-traffic-model
  - green-traffic-core
  - influxdb-client-java
  - rocketmq-spring-boot-starter

- green-traffic-simulator (profile: with-simulator)
  - green-traffic-common (optional)
  - green-traffic-model
  - green-traffic-core

- green-traffic-push
  - green-traffic-common (optional)
  - green-traffic-model
  - influxdb-client-java

- green-traffic-common
  - spring-boot-starter

## 建议的收敛与后续操作
1. 将 `green-traffic-common` 中仅被工具使用的类（如 `TimezoneUtils`）迁移到 `green-traffic-core` 或 `green-traffic-model`，以彻底切断 runtime 依赖。  
2. 保留 `green-traffic-common` 作为可选适配器/工具库，仅在需要时通过 profile 或父模块显式包含。  
3. 在 CI 中默认不激活 `with-simulator` profile，提供一个 `with-simulator` 构建任务用于开发和集成测试。  
4. 运行 `mvn -DactiveProfiles=with-simulator -pl green-traffic-simulator -am package` 做带仿真模块的全量测试构建。  

---

要我现在运行一次打包验证（跳过测试），并生成模块间依赖树吗？
