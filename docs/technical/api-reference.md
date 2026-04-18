# Mayfly - API 参考文档

**文档版本**：v1.0  
**编制日期**：2026-04-15  
**适用版本**：Mayfly 1.0.x

---

## 一、核心 API

### 1.1 ModelRouter（模型路由器）

**包路径**：`io.mayfly.core.ModelRouter`

**说明**：模型路由核心接口，所有模型调用的统一入口

**方法**：

#### chat(ChatRequest request)

```java
/**
 * 同步调用模型
 * 
 * @param request 聊天请求
 * @return 聊天响应
 * @throws ModelUnavailableException 当没有可用模型时
 * @throws RoutingException 路由失败时
 */
ChatResponse chat(ChatRequest request);
```

**使用示例**：
```java
@Service
public class ChatService {
    
    @Autowired
    private ModelRouter modelRouter;
    
    public String sendMessage(String message) {
        ChatRequest request = new ChatRequest(new Prompt(message));
        ChatResponse response = modelRouter.chat(request);
        return response.getResult().getOutput().getContent();
    }
}
```

#### stream(ChatRequest request)

```java
/**
 * 流式调用模型
 * 
 * @param request 聊天请求
 * @return 聊天响应流
 */
Flux<ChatResponse> stream(ChatRequest request);
```

**使用示例**：
```java
public Flux<ChatResponse> streamMessage(String message) {
    ChatRequest request = new ChatRequest(new Prompt(message));
    return modelRouter.stream(request);
}
```

#### async(ChatRequest request)

```java
/**
 * 异步调用模型
 * 
 * @param request 聊天请求
 * @return 聊天响应 Future
 */
CompletableFuture<ChatResponse> async(ChatRequest request);
```

**使用示例**：
```java
public CompletableFuture<ChatResponse> asyncMessage(String message) {
    ChatRequest request = new ChatRequest(new Prompt(message));
    return modelRouter.async(request);
}
```

---

### 1.2 ModelRegistry（模型注册中心）

**包路径**：`io.mayfly.core.ModelRegistry`

**说明**：管理所有可用模型实例

**方法**：

#### register(ModelConfig config)

```java
/**
 * 注册模型
 * 
 * @param config 模型配置
 * @throws IllegalArgumentException 配置为空时
 */
void register(ModelConfig config);
```

#### getModel(String name)

```java
/**
 * 获取模型实例
 * 
 * @param name 模型名称
 * @return 模型实例，不存在时返回 Optional.empty()
 */
Optional<ModelInstance> getModel(String name);
```

#### getAllAvailableModels()

```java
/**
 * 获取所有可用模型
 * 
 * @return 可用模型列表
 */
List<ModelInstance> getAllAvailableModels();
```

#### updateModel(String name, ModelConfig config)

```java
/**
 * 更新模型配置
 * 
 * @param name 模型名称
 * @param config 新配置
 */
void updateModel(String name, ModelConfig config);
```

#### removeModel(String name)

```java
/**
 * 移除模型
 * 
 * @param name 模型名称
 */
void removeModel(String name);
```

#### getModelsByTag(String tag)

```java
/**
 * 根据标签获取模型
 * 
 * @param tag 标签
 * @return 匹配的模型列表
 */
List<ModelInstance> getModelsByTag(String tag);
```

---

### 1.3 RouterStrategy（路由策略）

**包路径**：`io.mayfly.router.RouterStrategy`

**说明**：路由策略接口

**方法**：

#### select(ChatRequest request, List<ModelInstance> candidates)

```java
/**
 * 从候选模型中选择一个
 * 
 * @param request 聊天请求
 * @param candidates 候选模型列表
 * @return 选中的模型实例
 * @throws ModelUnavailableException 没有可用模型时
 */
ModelInstance select(ChatRequest request, List<ModelInstance> candidates);
```

#### getName()

```java
/**
 * 获取策略名称
 * 
 * @return 策略名称
 */
String getName();
```

#### getOrder()

```java
/**
 * 获取策略优先级
 * 
 * @return 优先级（数字越小优先级越高）
 */
default int getOrder() {
    return 100;
}
```

---

### 1.4 ModelAdapter（模型适配器）

**包路径**：`io.mayfly.adapter.ModelAdapter`

**说明**：模型适配器接口

**方法**：

#### createChatModel(ModelConfig config)

```java
/**
 * 创建 ChatModel 实例
 * 
 * @param config 模型配置
 * @return ChatModel 实例
 * @throws IllegalArgumentException 配置无效时
 */
ChatModel createChatModel(ModelConfig config);
```

#### getProvider()

```java
/**
 * 获取支持的提供商
 * 
 * @return 提供商名称
 */
String getProvider();
```

---

## 二、实体类 API

### 2.1 ModelConfig

**包路径**：`io.mayfly.core.ModelConfig`

**说明**：模型配置实体

**属性**：

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | 是 | - | 模型唯一标识 |
| provider | String | 是 | - | 模型提供商 |
| model | String | 是 | - | 模型 ID |
| apiKey | String | 是 | - | API Key |
| baseUrl | String | 否 | - | API Base URL |
| weight | int | 否 | 100 | 权重 |
| enabled | boolean | 否 | true | 是否启用 |
| timeout | long | 否 | 30000 | 超时时间 (ms) |
| maxRetries | int | 否 | 2 | 最大重试次数 |
| tags | List<String> | 否 | [] | 模型标签 |
| properties | Map<String, Object> | 否 | {} | 额外参数 |

**构建示例**：
```java
ModelConfig config = ModelConfig.builder()
    .name("zhipu-primary")
    .provider("zhipu")
    .model("glm-4")
    .apiKey("your-api-key")
    .weight(70)
    .timeout(30000L)
    .maxRetries(2)
    .tags(Arrays.asList("primary", "chinese"))
    .build();
```

---

### 2.2 ModelInstance

**包路径**：`io.mayfly.core.ModelInstance`

**说明**：模型实例（运行时状态）

**属性**：

| 属性 | 类型 | 说明 |
|------|------|------|
| config | ModelConfig | 模型配置 |
| chatModel | ChatModel | 底层 ChatModel |
| healthStatus | HealthStatus | 健康状态 |
| activeRequests | AtomicInteger | 当前活跃请求数 |
| totalRequests | AtomicLong | 总请求数 |
| failedRequests | AtomicLong | 失败请求数 |
| avgLatency | AtomicLong | 平均响应时间 (ms) |
| lastHealthCheck | Instant | 最后健康检查时间 |
| cooldownUntil | Instant | 冷却结束时间 |

**方法**：

#### recordSuccess(long latency)

```java
/**
 * 记录请求成功
 * 
 * @param latency 响应时间 (ms)
 */
void recordSuccess(long latency);
```

#### recordFailure()

```java
/**
 * 记录请求失败
 */
void recordFailure();
```

#### isAvailable()

```java
/**
 * 检查是否可用
 * 
 * @return true-可用，false-不可用
 */
boolean isAvailable();
```

#### getFailureRate()

```java
/**
 * 获取失败率
 * 
 * @return 失败率 (0.0-1.0)
 */
double getFailureRate();
```

---

### 2.3 RouterRule

**包路径**：`io.mayfly.router.RouterRule`

**说明**：路由规则实体

**属性**：

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| name | String | 是 | - | 规则名称 |
| condition | String | 是 | - | SpEL 条件表达式 |
| targetModel | String | 是 | - | 目标模型名称 |
| priority | int | 否 | 100 | 优先级 |

**构建示例**：
```java
RouterRule rule = RouterRule.builder()
    .name("vip-users")
    .condition("#request.metadata?.userType == 'VIP'")
    .targetModel("zhipu-primary")
    .priority(1)
    .build();
```

---

## 三、配置属性 API

### 3.1 MayflyProperties

**包路径**：`io.mayfly.autoconfigure.MayflyProperties`

**说明**：Mayfly 配置属性类

**配置前缀**：`mayfly`

**属性**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| enabled | boolean | true | 是否启用 Mayfly |
| models | List<ModelConfig> | [] | 模型配置列表 |
| router.strategy | String | "fixed" | 路由策略 |
| router.rules | List<RouterRule> | [] | 路由规则列表 |
| failover.enabled | boolean | true | 是否启用故障转移 |
| failover.maxRetries | int | 2 | 最大重试次数 |
| failover.cooldownDuration | Duration | 60s | 冷却时长 |
| loadbalancer.strategy | String | "round-robin" | 负载均衡策略 |
| circuitBreaker.enabled | boolean | true | 是否启用熔断器 |
| rateLimiter.enabled | boolean | true | 是否启用限流器 |
| monitor.enabled | boolean | true | 是否启用监控 |

---

## 四、异常类 API

### 4.1 MayflyException

**包路径**：`io.mayfly.core.MayflyException`

**说明**：Mayfly 基础异常类

**继承**：`RuntimeException`

**构造方法**：
```java
public MayflyException(String message);
public MayflyException(String message, Throwable cause);
```

---

### 4.2 ModelUnavailableException

**包路径**：`io.mayfly.core.ModelUnavailableException`

**说明**：模型不可用异常

**继承**：`MayflyException`

**使用场景**：
- 没有可用模型时
- 模型配置错误时

---

### 4.3 RoutingException

**包路径**：`io.mayfly.core.RoutingException`

**说明**：路由失败异常

**继承**：`MayflyException`

**使用场景**：
- 路由策略选择失败时
- 路由规则匹配失败时

---

## 五、枚举类 API

### 5.1 HealthStatus

**包路径**：`io.mayfly.core.HealthStatus`

**说明**：健康状态枚举

**枚举值**：

| 枚举值 | 说明 |
|--------|------|
| HEALTHY | 健康 |
| UNHEALTHY | 不健康 |
| COOLDOWN | 冷却中 |

---

## 六、监控指标 API

### 6.1 MetricsCollector

**包路径**：`io.mayfly.monitor.MetricsCollector`

**说明**：监控指标收集器接口

**方法**：

#### recordSuccess

```java
/**
 * 记录调用成功
 * 
 * @param modelName 模型名称
 * @param latencyMs 延迟 (ms)
 * @param inputTokens 输入 Token 数
 * @param outputTokens 输出 Token 数
 */
void recordSuccess(String modelName, long latencyMs, 
                   int inputTokens, int outputTokens);
```

#### recordFailure

```java
/**
 * 记录调用失败
 * 
 * @param modelName 模型名称
 * @param errorType 错误类型
 */
void recordFailure(String modelName, String errorType);
```

#### recordFailover

```java
/**
 * 记录故障转移
 * 
 * @param fromModel 源模型
 * @param toModel 目标模型
 */
void recordFailover(String fromModel, String toModel);
```

---

**文档版本**：v1.0  
**更新日期**：2026-04-15
