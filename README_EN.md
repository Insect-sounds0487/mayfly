# Mayfly - Enterprise Model Router Enhancement Plugin for Spring AI

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0%2B-blue.svg)](https://spring.io/projects/spring-ai)

> **Empower every Spring AI Java developer with enterprise-grade model routing capabilities at zero cost.**

---

## 📖 Introduction

Mayfly is an enterprise-grade model routing enhancement plugin based on Spring AI, providing out-of-the-box load balancing, failover, circuit breaking, and other enterprise capabilities for Chinese Java developers, with deep integration support for domestic models (ZhiPu, Tongyi, DeepSeek, etc.).

### ✨ Core Features

| Feature | Description |
|---------|-------------|
| 🔄 **Unified Multi-Model API** | Single interface to call different vendor models, hiding API differences |
| 🎯 **Intelligent Routing** | Supports fixed, weighted, and rule-based (SpEL) routing strategies |
| ⚖️ **Load Balancing** | Round-robin and weighted round-robin load balancing algorithms |
| 🛡️ **Failover** | Automatic failover to backup models with cooldown mechanism |
| 🔌 **Circuit Breaking** | Circuit breaker and rate limiter based on Resilience4j |
| 📊 **Monitoring & Observability** | Complete monitoring metrics based on Micrometer |
| 🇨🇳 **Domestic Model Integration** | Deep integration with ZhiPu, Tongyi Qwen, DeepSeek, and other domestic models |
| 🚀 **Zero-Configuration Integration** | Spring Boot Starter auto-configuration, minimal setup in just 3 lines |

---

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.mayfly</groupId>
    <artifactId>mayfly-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Models

Add configuration in `application.yml`:

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

### 3. Use Routing

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

That's it! Mayfly automatically handles routing, load balancing, failover, and all complex logic.

---

## 📋 Complete Configuration

```yaml
mayfly:
  enabled: true
  
  # Model Configuration
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
  
  # Routing Configuration
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
  
  # Load Balancer Configuration
  loadbalancer:
    strategy: weighted-round-robin  # round-robin, weighted-round-robin
    health-check:
      enabled: true
      interval: 30s
      timeout: 5s
      unhealthy-threshold: 3
  
  # Failover Configuration
  failover:
    enabled: true
    max-retries: 2
    cooldown-duration: 60s
    retryable-exceptions:
      - java.net.SocketTimeoutException
      - org.springframework.web.client.HttpServerErrorException
  
  # Circuit Breaker Configuration
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50
    wait-duration-in-open-state: 60s
    sliding-window-size: 10
    minimum-number-of-calls: 5
  
  # Rate Limiter Configuration
  rate-limiter:
    enabled: true
    limit-refresh-period: 1s
    limit-for-period: 100
    timeout-duration: 0s
  
  # Monitoring Configuration
  monitor:
    enabled: true
```

---

## 🤝 Open Source Community

We welcome all forms of contributions! Please check our [Contribution Guide](CONTRIBUTING.md) to learn how to participate in project development.

### 📄 Documentation
- [Contribution Guide](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)  
- [Changelog](CHANGELOG.md)

### 🐛 Issues & Discussion
- **Issues**: [Submit issues or feature requests](https://github.com/mayfly-ai/mayfly/issues)
- **Email**: git@xsjyby.asia

---

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).