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

> 💡 完整示例配置请参考 [application-example.yml](https://gitcode.com/Topfogking/mayfly/blob/master/mayfly-demo/src/main/resources/application-example.yml)

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
        Prompt prompt = new Prompt(message);
        return modelRouter.chat(prompt);
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

## 📚 文档

- [用户手册](https://gitcode.com/Topfogking/mayfly/blob/master/docs/user-guide.md)
- [API参考](https://gitcode.com/Topfogking/mayfly/blob/master/docs/technical/api-reference.md)
- [部署指南](https://gitcode.com/Topfogking/mayfly/blob/master/docs/technical/deployment-guide.md)
- [开发手册](https://gitcode.com/Topfogking/mayfly/blob/master/docs/technical/developer-guide.md)
- [示例配置](https://gitcode.com/Topfogking/mayfly/blob/master/mayfly-demo/src/main/resources/application-example.yml)

---

## 🤝 开源社区

我们欢迎所有形式的贡献！请查看我们的 [贡献指南](CONTRIBUTING.md) 了解如何参与项目开发。

### 📄 其他文档
- [贡献指南](CONTRIBUTING.md)
- [行为准则](CODE_OF_CONDUCT.md)  
- [版本日志](CHANGELOG.md)

### 🐛 问题与讨论
- **Issues**: [提交问题或功能请求](https://gitcode.com/Topfogking/mayfly/issues)
- **邮箱**: git@xsjyby.asia

---

## 🌍 多语言支持

- 🇨🇳 [中文文档 (Chinese)](README.md)
- 🇺🇸 [English Documentation](README_EN.md)

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。