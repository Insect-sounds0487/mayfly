# Mayfly - 基于Spring AI的企业级模型路由增强插件

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0%2B-blue.svg)](https://spring.io/projects/spring-ai)

> **让每个使用Spring AI的Java开发者，都能零成本获得企业级模型路由能力。**

---

## 📖 简介

Mayfly是一个基于Spring AI的企业级模型路由增强插件，为国内Java开发者提供开箱即用的负载均衡、故障转移、熔断限流等企业级能力，深度适配国产模型（智谱、通义、DeepSeek等）。

### ✨ 核心特性

| 特性 | 说明 |
|------|------|
| 🔄 **多模型统一调用** | 统一接口调用不同厂商模型，屏蔽API差异 |
| 🎯 **智能路由** | 支持固定、权重、规则（SpEL）三种路由策略 |
| ⚖️ **负载均衡** | 轮询、加权轮询等负载均衡算法 |
| 🛡️ **故障转移** | 主模型故障自动切换备用模型，支持冷却机制 |
| 🔌 **熔断限流** | 基于Resilience4j的熔断器和限流器 |
| 📊 **监控可观测** | 基于Micrometer的完整监控指标 |
| 🇨🇳 **国产模型适配** | 深度适配智谱、通义千问、DeepSeek等国产模型 |
| 🚀 **零配置接入** | Spring Boot Starter自动配置，最小配置仅需3行 |

---

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.mayfly</groupId>
    <artifactId>mayfly-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置模型

在`application.yml`中添加配置：

```yaml
mayfly:
  models:
    - name: zhipu-primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
      weight: 70
    
    - name: tongyi-backup
      provider: tongyi
      api-key: ${TONGYI_API_KEY}
      model: qwen-max
      weight: 30
```

### 3. 使用路由

```java
@Service
public class ChatService {
    
    private final ModelRouter modelRouter;
    
    public ChatService(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }
    
    public ChatResponse chat(String message) {
        ChatRequest request = new ChatRequest(new Prompt(message));
        return modelRouter.chat(request);
    }
}
```

就这么简单！Mayfly会自动处理路由、负载均衡、故障转移等所有复杂逻辑。

---

## 📋 完整配置

```yaml
mayfly:
  enabled: true
  
  # 模型配置
  models:
    - name: zhipu-primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
      weight: 70
      timeout: 30000
      max-retries: 2
      tags:
        - primary
        - chinese
    
    - name: tongyi-backup
      provider: tongyi
      api-key: ${TONGYI_API_KEY}
      model: qwen-max
      weight: 30
      tags:
        - backup
    
    - name: deepseek-coder
      provider: deepseek
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-coder
      tags:
        - code
  
  # 路由配置
  router:
    strategy: rule-based  # fixed, weighted, rule-based
    rules:
      - name: vip-users
        condition: "#request.metadata?.userType == 'VIP'"
        target-model: zhipu-primary
        priority: 1
      - name: code-tasks
        condition: "#request.metadata?.taskType == 'CODE'"
        target-model: deepseek-coder
        priority: 2
      - name: default
        condition: "true"
        target-model: zhipu-primary
        priority: 99
  
  # 负载均衡配置
  loadbalancer:
    strategy: weighted-round-robin  # round-robin, weighted-round-robin
    health-check:
      enabled: true
      interval: 30s
      timeout: 5s
      unhealthy-threshold: 3
  
  # 故障转移配置
  failover:
    enabled: true
    max-retries: 2
    cooldown-duration: 60s
    retryable-exceptions:
      - java.net.SocketTimeoutException
      - org.springframework.web.client.HttpServerErrorException
  
  # 熔断器配置
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50
    wait-duration-in-open-state: 60s
    sliding-window-size: 10
    minimum-number-of-calls: 5
  
  # 限流器配置
  rate-limiter:
    enabled: true
    limit-refresh-period: 1s
    limit-for-period: 100
    timeout-duration: 0s
  
  # 监控配置
  monitor:
    enabled: true
```

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────┐
│         业务应用层                    │
│      (User's Spring Boot App)        │
├─────────────────────────────────────┤
│      Mayfly 路由增强插件              │
│  ┌─────────┬─────────┬──────────┐   │
│  │ 负载均衡 │ 灰度发布 │ 熔断限流  │   │
│  ├─────────┼─────────┼──────────┤   │
│  │ 智能路由 │ 故障转移 │ 监控观测  │   │
│  └─────────┴─────────┴──────────┘   │
├─────────────────────────────────────┤
│         Spring AI 官方框架            │
├─────────────────────────────────────┤
│    国产模型 + 海外模型 适配层          │
└─────────────────────────────────────┘
```

---

## 📦 模块说明

| 模块 | 说明 |
|------|------|
| mayfly-core | 核心模块 - 模型路由核心接口和实体 |
| mayfly-router | 路由策略模块 - 固定/权重/规则路由策略 |
| mayfly-loadbalancer | 负载均衡模块 - 轮询/加权轮询 |
| mayfly-failover | 故障转移模块 - 故障转移和冷却机制 |
| mayfly-circuitbreaker | 熔断限流模块 - 基于Resilience4j |
| mayfly-adapter | 模型适配器模块 - 智谱/通义/DeepSeek |
| mayfly-monitor | 监控模块 - 基于Micrometer |
| mayfly-spring-boot-starter | Spring Boot Starter - 自动配置 |

---

## 📊 监控指标

Mayfly内置以下Prometheus指标：

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `mayfly.model.calls.total` | Counter | 模型调用总次数 |
| `mayfly.model.calls.success` | Counter | 成功调用次数 |
| `mayfly.model.calls.failure` | Counter | 失败调用次数 |
| `mayfly.model.calls.failover` | Counter | 故障转移次数 |
| `mayfly.model.latency.seconds` | Timer | 调用延迟 |
| `mayfly.model.tokens.input` | DistributionSummary | 输入Token数 |
| `mayfly.model.tokens.output` | DistributionSummary | 输出Token数 |

---

## 🤝 扩展开发

### 自定义路由策略

```java
@Component
public class CustomRouterStrategy implements RouterStrategy {
    
    @Override
    public ModelInstance select(ChatRequest request, List<ModelInstance> candidates) {
        // 自定义路由逻辑
        return candidates.stream()
            .filter(ModelInstance::isAvailable)
            .findFirst()
            .orElseThrow(() -> new ModelUnavailableException("No available model"));
    }
    
    @Override
    public String getName() {
        return "custom";
    }
}
```

### 自定义模型适配器

```java
@Component
public class CustomModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        // 创建自定义ChatModel
        return new CustomChatModel(config);
    }
    
    @Override
    public String getProvider() {
        return "custom";
    }
}
```

---

## 📄 文档

| 文档 | 说明 |
|------|------|
| [市场分析报告](docs/market-analysis.md) | 市场调研和商业可行性分析 |
| [产品需求文档](docs/prd.md) | MVP版本产品需求规格说明书 |
| [架构设计文档](docs/architecture.md) | 系统架构和技术方案设计 |
| [代码审查报告](docs/code-review-report-v2.md) | v2.0代码审查报告 |
| [测试报告](docs/test-report.md) | 测试用例和测试报告 |
| [资金预算](docs/budget.md) | 开发资金预算与使用计划 |
| [用户手册](docs/user-guide.md) | 用户使用指南 |
| [开发手册](docs/developer-guide.md) | 开发者指南 |

---

## 🛠️ 构建和运行

### 环境要求

- JDK 17+
- Maven 3.8+
- Spring Boot 3.2+

### 本地构建

```bash
# 克隆项目
git clone https://github.com/mayfly-ai/mayfly.git
cd mayfly

# 构建项目
mvn clean install

# 运行测试
mvn test
```

### Docker运行

```bash
# 构建镜像
docker build -t mayfly:latest .

# 运行容器
docker-compose up -d
```

---

## 📈 路线图

| 版本 | 功能 | 状态 |
|------|------|------|
| v0.1.0-alpha | P0功能（核心路由、故障转移、自动配置） | ✅ 完成 |
| v0.2.0-beta | P1功能（负载均衡、熔断限流、监控） | ✅ 完成 |
| v1.0.0-RC | 完整MVP+文档 | 🚧 进行中 |
| v1.0.0-GA | 正式发布 | 📋 计划中 |
| v1.1.0 | 灰度发布、高级路由策略 | 📋 计划中 |
| v2.0.0 | 管理后台、更多模型支持 | 📋 计划中 |

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

---

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

---

## 📞 联系我们

- GitHub Issues: [提交问题](https://github.com/mayfly-ai/mayfly/issues)
- 邮箱: dev@mayfly.io

---

**Made with ❤️ by Mayfly Team**
