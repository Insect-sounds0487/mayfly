# 变更日志

所有 Mayfly 的显著变更都将记录在此文件中。

## [1.1.0] - 2026-04-20

### 测试质量提升
- **全面单元测试**: 337 个测试用例，覆盖所有核心模块
  - mayfly-core: 28 个测试
  - mayfly-router: 40 个测试
  - mayfly-loadbalancer: 22 个测试
  - mayfly-failover: 38 个测试
  - mayfly-circuitbreaker: 36 个测试
  - mayfly-monitor: 18 个测试
  - mayfly-adapter: 109 个测试
  - mayfly-spring-boot-starter: 46 个测试
- **JaCoCo 覆盖率**: 75% 指令覆盖率，57% 分支覆盖率
- **PITest 变异测试**: 89% 变异覆盖率，94% 测试强度
- **异常场景测试**: 为 router、circuitbreaker、failover 模块添加完整异常测试
- **边界条件测试**: 覆盖 null 输入、空集合、极端值等边界情况

### 集成测试
- **WireMock 集成**: 使用 WireMock 模拟真实 HTTP 服务进行集成测试
- **端到端测试**: 完整的调用链路测试（路由 → 负载均衡 → 适配器 → 响应）
- **Mock HTTP 测试**: 为 adapter 模块添加 Mock HTTP 调用测试

### 性能测试
- **基准测试**: 适配器性能基准测试
  - 简单 prompt: ~3.66 μs/op
  - 大型 prompt: ~2.32 μs/op

### CI/CD 增强
- **GitHub Actions 流水线**:
  - 代码质量检查（编译 + 代码风格）
  - 单元测试 + JaCoCo 覆盖率报告
  - 集成测试（WireMock）
  - 变异测试（PITest）
  - 性能基准测试
  - 构建 + Docker 镜像推送
- **并行执行**: test、integration-test、performance-test、mutation-test 并行运行
- **测试结果归档**: 所有测试结果自动保存为 artifacts

### 代码改进
- **可测试性架构**: 创建 HttpClient 接口，支持 Mock 测试
- **依赖注入优化**: 改进测试依赖注入方式
- **测试工具类**: 创建通用的测试辅助方法

### 文档更新
- **测试指南**: 添加测试运行和覆盖率报告生成说明
- **CI/CD 文档**: 更新持续集成配置说明

## [1.0.0] - 2026-04-16

### 新增功能
- **国产模型深度适配**: 完整支持智谱 AI (GLM-4)、通义千问 (Qwen-Max)、DeepSeek 等国产大模型
- **多模型统一调用**: 提供统一的 `ModelRouter` 接口，屏蔽各厂商 API 差异
- **智能权重路由**: 支持基于权重的负载均衡分配（40% DeepSeek, 25% 智谱, 15% 通义, 20% 备份）
- **企业级特性**:
  - 负载均衡：加权轮询算法
  - 故障转移：主模型失败自动切换备用
  - 熔断保护：50% 失败率阈值，防止雪崩效应
  - 自动重试：最多 2 次重试机制
  - 健康监控：实时状态检查和性能指标

### 架构改进
- **模块化设计**: 8 个独立 Maven 模块，高内聚低耦合
  - mayfly-core: 核心接口和实体
  - mayfly-adapter: 模型适配器层
  - mayfly-router: 路由策略实现
  - mayfly-loadbalancer: 负载均衡算法
  - mayfly-failover: 故障转移机制
  - mayfly-circuitbreaker: 熔断限流
  - mayfly-monitor: 监控指标收集
  - mayfly-spring-boot-starter: 自动配置
- **Spring Boot Starter**: 零配置自动集成，最小配置仅需 3 行
- **Docker 支持**: 完整的容器化部署方案

### 配置增强
- **灵活配置**: 支持 YAML 配置文件，所有参数可自定义
- **安全设计**: API 密钥使用环境变量占位符 `${API_KEY}`，无敏感信息泄露风险
- **多路由策略**: 支持固定、权重、规则（SpEL）三种路由策略

### 测试与质量
- **单元测试**: 核心模块包含单元测试
- **集成测试**: 提供完整的企业级特性测试端点
- **代码质量**: 符合 Java 编码规范，注释完整

### 文档完善
- **用户手册**: 精简的 README，聚焦用户使用指南
- **快速开始**: 3 步上手指南
- **完整配置**: 详细的 YAML 配置示例

### 技术栈
- **Java 17+**: 最新 LTS 版本
- **Spring Boot 3.2+**: 最新稳定版本  
- **Spring AI 1.0+**: 完美兼容 Spring AI 生态
- **Resilience4j**: 企业级容错能力
- **Micrometer**: 完整监控指标

## [0.2.0-beta] - 2026-04-15

### 新增功能
- 负载均衡模块实现
- 熔断限流模块集成
- 监控指标收集

## [0.1.0-alpha] - 2026-04-14

### 新增功能
- 核心路由功能
- 故障转移机制
- 自动配置模块

---

**注意**: 这个项目遵循 [Semantic Versioning](https://semver.org/) 规范。