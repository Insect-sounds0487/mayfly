# Mayfly - 用户手册

**文档版本**：v1.0  
**编制日期**：2026-04-15  
**适用版本**：Mayfly 1.0.x

---

## 一、快速入门

### 1.1 环境准备

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| JDK | 17 | 21 |
| Maven | 3.8 | 3.9 |
| Spring Boot | 3.2 | 3.2.4 |
| Spring AI | 1.0.0-M6 | 1.0.0-M6 |

### 1.2 添加依赖

在项目的`pom.xml`中添加：

```xml
<dependency>
    <groupId>io.mayfly</groupId>
    <artifactId>mayfly-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1.3 最小配置

在`application.yml`中配置至少一个模型：

```yaml
mayfly:
  models:
    - name: my-model
      provider: zhipu
      api-key: your-api-key-here
      model: glm-4
```

### 1.4 开始使用

```java
@Service
public class MyService {
    
    private final ModelRouter modelRouter;
    
    public MyService(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }
    
    public String chat(String message) {
        ChatRequest request = new ChatRequest(new Prompt(message));
        ChatResponse response = modelRouter.chat(request);
        return response.getResult().getOutput().getText();
    }
}
```

---

## 二、配置详解

### 2.1 模型配置

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| name | String | 是 | - | 模型唯一标识 |
| provider | String | 是 | - | 模型提供商（zhipu/tongyi/deepseek） |
| api-key | String | 是 | - | API密钥 |
| model | String | 是 | - | 模型ID |
| base-url | String | 否 | 官方默认 | API基础URL |
| weight | int | 否 | 100 | 权重（用于负载均衡） |
| enabled | boolean | 否 | true | 是否启用 |
| timeout | long | 否 | 30000 | 超时时间（毫秒） |
| max-retries | int | 否 | 2 | 最大重试次数 |
| tags | List<String> | 否 | [] | 模型标签 |
| properties | Map | 否 | {} | 额外参数 |

### 2.2 路由策略

**固定路由（fixed）**：
```yaml
mayfly:
  router:
    strategy: fixed
```
始终使用第一个可用的模型。

**权重路由（weighted）**：
```yaml
mayfly:
  router:
    strategy: weighted
  models:
    - name: model-a
      weight: 70
    - name: model-b
      weight: 30
```
按权重比例分配请求（70%→model-a，30%→model-b）。

**规则路由（rule-based）**：
```yaml
mayfly:
  router:
    strategy: rule-based
    rules:
      - name: vip-rule
        condition: "#request.metadata?.userType == 'VIP'"
        target-model: premium-model
        priority: 1
      - name: default
        condition: "true"
        target-model: default-model
        priority: 99
```
基于SpEL表达式进行条件路由。

### 2.3 负载均衡

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| round-robin | 轮询 | 模型性能相近 |
| weighted-round-robin | 加权轮询 | 模型性能差异大 |

### 2.4 故障转移

```yaml
mayfly:
  failover:
    enabled: true
    max-retries: 2
    cooldown-duration: 60s
    retryable-exceptions:
      - java.net.SocketTimeoutException
```

当主模型调用失败时：
1. 标记模型进入冷却期（60秒内不参与路由）
2. 自动切换到备用模型
3. 最多重试2次

### 2.5 熔断限流

```yaml
mayfly:
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 50
    wait-duration-in-open-state: 60s
    sliding-window-size: 10
    minimum-number-of-calls: 5
  
  rate-limiter:
    enabled: true
    limit-refresh-period: 1s
    limit-for-period: 100
```

**熔断器状态**：
- Closed（关闭）：正常调用
- Open（打开）：拒绝调用，等待恢复
- Half-Open（半开）：尝试恢复

---

## 三、使用场景

### 3.1 智能客服

```yaml
mayfly:
  models:
    - name: primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
      weight: 80
    
    - name: backup
      provider: tongyi
      api-key: ${TONGYI_API_KEY}
      model: qwen-max
      weight: 20
  
  router:
    strategy: weighted
  
  failover:
    enabled: true
    max-retries: 2
```

### 3.2 代码助手

```yaml
mayfly:
  models:
    - name: coder
      provider: deepseek
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-coder
    
    - name: general
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
  
  router:
    strategy: rule-based
    rules:
      - name: code-task
        condition: "#request.metadata?.taskType == 'CODE'"
        target-model: coder
        priority: 1
      - name: default
        condition: "true"
        target-model: general
        priority: 99
```

### 3.3 VIP用户优先

```yaml
mayfly:
  models:
    - name: premium
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
    
    - name: standard
      provider: deepseek
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-chat
  
  router:
    strategy: rule-based
    rules:
      - name: vip
        condition: "#request.metadata?.userType == 'VIP'"
        target-model: premium
        priority: 1
      - name: default
        condition: "true"
        target-model: standard
        priority: 99
```

---

## 四、监控与运维

### 4.1 查看监控指标

启动应用后，访问`http://localhost:8080/actuator/prometheus`查看指标。

### 4.2 常用查询

```bash
# 查看模型调用成功率
mayfly_model_calls_success / mayfly_model_calls_total

# 查看平均延迟
rate(mayfly_model_latency_seconds_sum[5m]) / rate(mayfly_model_latency_seconds_count[5m])

# 查看故障转移次数
increase(mayfly_model_calls_failover_total[1h])
```

### 4.3 日志配置

```yaml
logging:
  level:
    io.mayfly: DEBUG  # 查看详细路由日志
```

---

## 五、常见问题

### Q1: 如何切换模型提供商？

修改配置中的`provider`字段即可：
```yaml
mayfly:
  models:
    - name: my-model
      provider: tongyi  # 从zhipu改为tongyi
      api-key: ${TONGYI_API_KEY}
      model: qwen-max
```

### Q2: 如何实现灰度发布？

使用权重路由策略：
```yaml
mayfly:
  models:
    - name: v1
      weight: 90  # 90%流量
    - name: v2
      weight: 10  # 10%流量（新版本）
```

### Q3: 如何禁用某个模型？

设置`enabled: false`：
```yaml
mayfly:
  models:
    - name: old-model
      enabled: false  # 禁用此模型
```

### Q4: 如何自定义超时时间？

```yaml
mayfly:
  models:
    - name: my-model
      timeout: 60000  # 60秒
```

---

**文档版本**：v1.0  
**更新日期**：2026-04-15
