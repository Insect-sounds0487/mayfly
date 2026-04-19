# Mayfly - 系统架构设计文档

**文档版本**：v1.0  
**编制日期**：2026-04-15  
**编制Agent**：系统架构设计Agent（qwen3-coder-plus）  
**基于文档**：[docs/prd.md](file:///e:/Insect-sounds/mayfly/docs/prd.md)

---

## 一、架构概述

### 1.1 架构目标

1. **高内聚低耦合**：各模块职责清晰，依赖关系简单
2. **可扩展性**：支持新模型、新路由策略的插件式扩展
3. **高性能**：路由延迟<1ms，支持高并发场景
4. **易集成**：Spring Boot Starter零配置接入

### 1.2 架构原则

| 原则 | 说明 |
|------|------|
| 基于Spring AI | 构建在Spring AI之上，不重复造轮子 |
| 插件化设计 | 路由策略、负载均衡、熔断器均可插拔 |
| 配置驱动 | 所有行为通过配置控制，支持热更新 |
| 可观测性 | 内置监控指标，支持Prometheus |

---

## 二、整体架构

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        业务应用层                                 │
│                    (User's Spring Boot App)                      │
├─────────────────────────────────────────────────────────────────┤
│                      Mayfly 路由层                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  ModelRouter │  │RouterStrategy│  │    LoadBalancer         │  │
│  │  (路由门面)   │  │  (路由策略)   │  │    (负载均衡)            │  │
│  └──────┬──────┘  └──────┬──────┘  └────────────┬────────────┘  │
│         │                │                       │               │
│  ┌──────┴────────────────┴───────────────────────┴────────────┐  │
│  │                    ModelRegistry                            │  │
│  │                   (模型注册中心)                              │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │                                    │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │                  FailoverHandler                            │  │
│  │                  (故障转移处理器)                             │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │                                    │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │              CircuitBreaker + RateLimiter                   │  │
│  │              (熔断器 + 限流器)                                │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
├─────────────────────────────┼───────────────────────────────────┤
│                      Spring AI 适配层                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ ZhipuAdapter │  │TongyiAdapter│  │  DeepSeekAdapter        │  │
│  │  (智谱适配器)  │  │ (通义适配器)  │  │  (DeepSeek适配器)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                      模型提供商层                                  │
│         智谱AI          通义千问          DeepSeek                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块 | 包路径 | 职责 | 优先级 |
|------|--------|------|--------|
| mayfly-core | `io.mayfly.core` | 核心路由逻辑 | P0 |
| mayfly-router | `io.mayfly.router` | 路由策略实现 | P0 |
| mayfly-loadbalancer | `io.mayfly.loadbalancer` | 负载均衡实现 | P1 |
| mayfly-failover | `io.mayfly.failover` | 故障转移实现 | P0 |
| mayfly-circuitbreaker | `io.mayfly.circuitbreaker` | 熔断限流实现 | P1 |
| mayfly-adapter | `io.mayfly.adapter` | 模型适配器 | P0 |
| mayfly-monitor | `io.mayfly.monitor` | 监控指标 | P1 |
| mayfly-spring-boot-starter | `io.mayfly.autoconfigure` | 自动配置 | P0 |

---

## 三、核心模块设计

### 3.1 mayfly-core（核心模块）

#### 3.1.1 核心接口

```java
package io.mayfly.core;

/**
 * 模型路由核心接口
 * 所有模型调用的统一入口
 */
public interface ModelRouter {
    
    /**
     * 同步调用
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 流式调用
     * @param request 聊天请求
     * @return 聊天响应流
     */
    Flux<ChatResponse> stream(ChatRequest request);
    
    /**
     * 异步调用
     * @param request 聊天请求
     * @return 聊天响应Future
     */
    CompletableFuture<ChatResponse> async(ChatRequest request);
}
```

```java
package io.mayfly.core;

/**
 * 模型注册中心
 * 管理所有可用模型实例
 */
public interface ModelRegistry {
    
    /**
     * 注册模型
     * @param config 模型配置
     */
    void register(ModelConfig config);
    
    /**
     * 获取模型实例
     * @param name 模型名称
     * @return 模型实例
     */
    Optional<ModelInstance> getModel(String name);
    
    /**
     * 获取所有可用模型
     * @return 可用模型列表
     */
    List<ModelInstance> getAllAvailableModels();
    
    /**
     * 获取所有模型（包括不可用）
     * @return 所有模型列表
     */
    List<ModelInstance> getAllModels();
    
    /**
     * 更新模型配置
     * @param name 模型名称
     * @param config 新配置
     */
    void updateModel(String name, ModelConfig config);
    
    /**
     * 移除模型
     * @param name 模型名称
     */
    void removeModel(String name);
    
    /**
     * 根据标签获取模型
     * @param tag 标签
     * @return 匹配的模型列表
     */
    List<ModelInstance> getModelsByTag(String tag);
}
```

#### 3.1.2 核心实体

```java
package io.mayfly.core;

/**
 * 模型配置
 */
@Data
@Builder
public class ModelConfig {
    
    /** 模型唯一标识 */
    private String name;
    
    /** 模型提供商 (zhipu, tongyi, deepseek等) */
    private String provider;
    
    /** 模型ID (glm-4, qwen-max等) */
    private String model;
    
    /** API Key */
    private String apiKey;
    
    /** API Base URL (可选，用于私有化部署) */
    private String baseUrl;
    
    /** 权重 (用于负载均衡) */
    @Builder.Default
    private int weight = 100;
    
    /** 是否启用 */
    @Builder.Default
    private boolean enabled = true;
    
    /** 超时时间(毫秒) */
    @Builder.Default
    private long timeout = 30000;
    
    /** 最大重试次数 */
    @Builder.Default
    private int maxRetries = 2;
    
    /** 模型标签 (用于分组) */
    private List<String> tags = new ArrayList<>();
    
    /** 额外参数 */
    private Map<String, Object> properties = new HashMap<>();
}
```

```java
package io.mayfly.core;

/**
 * 模型实例（运行时状态）
 */
@Data
public class ModelInstance {
    
    /** 模型配置 */
    private final ModelConfig config;
    
    /** 底层ChatModel (Spring AI) */
    private final ChatModel chatModel;
    
    /** 健康状态 */
    private volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    
    /** 当前活跃请求数 */
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    /** 总请求数 */
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    /** 失败请求数 */
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /** 平均响应时间(毫秒) */
    private final AtomicLong avgLatency = new AtomicLong(0);
    
    /** 最后健康检查时间 */
    private volatile Instant lastHealthCheck;
    
    /** 冷却结束时间 */
    private volatile Instant cooldownUntil;
    
    /**
     * 记录请求成功
     */
    public void recordSuccess(long latency) {
        totalRequests.incrementAndGet();
        activeRequests.decrementAndGet();
        // 更新平均响应时间 (指数移动平均)
        long currentAvg = avgLatency.get();
        avgLatency.set((long) (currentAvg * 0.9 + latency * 0.1));
    }
    
    /**
     * 记录请求失败
     */
    public void recordFailure() {
        failedRequests.incrementAndGet();
        activeRequests.decrementAndGet();
    }
    
    /**
     * 是否可用
     */
    public boolean isAvailable() {
        if (!config.isEnabled()) {
            return false;
        }
        if (healthStatus == HealthStatus.UNHEALTHY) {
            return false;
        }
        if (healthStatus == HealthStatus.COOLDOWN && 
            Instant.now().isBefore(cooldownUntil)) {
            return false;
        }
        return true;
    }
}
```

```java
package io.mayfly.core;

/**
 * 健康状态枚举
 */
public enum HealthStatus {
    /** 健康 */
    HEALTHY,
    /** 不健康 */
    UNHEALTHY,
    /** 冷却中 */
    COOLDOWN
}
```

---

### 3.2 mayfly-router（路由策略模块）

#### 3.2.1 路由策略接口

```java
package io.mayfly.router;

/**
 * 路由策略接口
 */
public interface RouterStrategy {
    
    /**
     * 选择目标模型
     * @param request 聊天请求
     * @param candidates 候选模型列表
     * @return 选中的模型实例
     */
    ModelInstance select(ChatRequest request, List<ModelInstance> candidates);
    
    /**
     * 策略名称
     */
    String getName();
    
    /**
     * 策略优先级 (数字越小优先级越高)
     */
    default int getOrder() {
        return 100;
    }
}
```

#### 3.2.2 内置路由策略

```java
package io.mayfly.router.impl;

/**
 * 固定路由策略
 * 始终路由到指定的主模型
 */
@Component
public class FixedRouterStrategy implements RouterStrategy {
    
    @Override
    public ModelInstance select(ChatRequest request, List<ModelInstance> candidates) {
        return candidates.stream()
            .filter(ModelInstance::isAvailable)
            .findFirst()
            .orElseThrow(() -> new ModelUnavailableException("No available model"));
    }
    
    @Override
    public String getName() {
        return "fixed";
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
}
```

```java
package io.mayfly.router.impl;

/**
 * 权重路由策略
 * 按权重比例分配请求
 */
@Component
public class WeightedRouterStrategy implements RouterStrategy {
    
    private final Random random = new Random();
    
    @Override
    public ModelInstance select(ChatRequest request, List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        int totalWeight = available.stream()
            .mapToInt(m -> m.getConfig().getWeight())
            .sum();
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (ModelInstance model : available) {
            currentWeight += model.getConfig().getWeight();
            if (randomValue < currentWeight) {
                return model;
            }
        }
        
        return available.get(available.size() - 1);
    }
    
    @Override
    public String getName() {
        return "weighted";
    }
}
```

```java
package io.mayfly.router.impl;

/**
 * 规则路由策略
 * 基于SpEL表达式进行条件路由
 */
@Component
public class RuleBasedRouterStrategy implements RouterStrategy {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final List<RouterRule> rules = new ArrayList<>();
    
    @Override
    public ModelInstance select(ChatRequest request, List<ModelInstance> candidates) {
        // 按优先级排序规则
        rules.stream()
            .sorted(Comparator.comparingInt(RouterRule::getPriority))
            .forEach(rule -> {
                if (matches(rule, request)) {
                    // 找到匹配的规则，路由到目标模型
                    String targetModel = rule.getTargetModel();
                    return candidates.stream()
                        .filter(m -> m.getConfig().getName().equals(targetModel))
                        .filter(ModelInstance::isAvailable)
                        .findFirst()
                        .orElseThrow(() -> new ModelUnavailableException(
                            "Target model not available: " + targetModel));
                }
            });
        
        // 没有匹配的规则，使用默认策略
        throw new RoutingFailedException("No matching rule found");
    }
    
    private boolean matches(RouterRule rule, ChatRequest request) {
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("request", request);
        Expression expression = parser.parseExpression(rule.getCondition());
        return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
    }
    
    @Override
    public String getName() {
        return "rule-based";
    }
}
```

```java
package io.mayfly.router;

/**
 * 路由规则
 */
@Data
@Builder
public class RouterRule {
    
    /** 规则名称 */
    private String name;
    
    /** SpEL条件表达式 */
    private String condition;
    
    /** 目标模型名称 */
    private String targetModel;
    
    /** 优先级 (数字越小优先级越高) */
    @Builder.Default
    private int priority = 100;
}
```

---

### 3.3 mayfly-loadbalancer（负载均衡模块）

#### 3.3.1 负载均衡接口

```java
package io.mayfly.loadbalancer;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {
    
    /**
     * 选择目标模型
     * @param candidates 候选模型列表
     * @return 选中的模型实例
     */
    ModelInstance choose(List<ModelInstance> candidates);
    
    /**
     * 负载均衡器名称
     */
    String getName();
}
```

#### 3.3.2 内置负载均衡器

```java
package io.mayfly.loadbalancer.impl;

/**
 * 轮询负载均衡器
 */
@Component
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ModelInstance choose(List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        int index = Math.abs(counter.getAndIncrement() % available.size());
        return available.get(index);
    }
    
    @Override
    public String getName() {
        return "round-robin";
    }
}
```

```java
package io.mayfly.loadbalancer.impl;

/**
 * 加权轮询负载均衡器
 * 使用平滑加权轮询算法
 */
@Component
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    
    private final Map<String, WeightedNode> nodeMap = new ConcurrentHashMap<>();
    
    @Override
    public ModelInstance choose(List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        // 初始化或更新节点
        for (ModelInstance instance : available) {
            nodeMap.computeIfAbsent(instance.getConfig().getName(), 
                k -> new WeightedNode(instance));
            WeightedNode node = nodeMap.get(instance.getConfig().getName());
            node.setWeight(instance.getConfig().getWeight());
        }
        
        // 平滑加权轮询
        WeightedNode selected = null;
        int totalWeight = 0;
        
        for (WeightedNode node : nodeMap.values()) {
            if (!node.getInstance().isAvailable()) {
                continue;
            }
            node.setCurrentWeight(node.getCurrentWeight() + node.getWeight());
            totalWeight += node.getWeight();
            
            if (selected == null || 
                node.getCurrentWeight() > selected.getCurrentWeight()) {
                selected = node;
            }
        }
        
        if (selected != null) {
            selected.setCurrentWeight(selected.getCurrentWeight() - totalWeight);
            return selected.getInstance();
        }
        
        return available.get(0);
    }
    
    @Override
    public String getName() {
        return "weighted-round-robin";
    }
    
    @Data
    private static class WeightedNode {
        private final ModelInstance instance;
        private int weight;
        private int currentWeight;
        
        public WeightedNode(ModelInstance instance) {
            this.instance = instance;
            this.weight = instance.getConfig().getWeight();
            this.currentWeight = 0;
        }
    }
}
```

---

### 3.4 mayfly-failover（故障转移模块）

```java
package io.mayfly.failover;

/**
 * 故障转移处理器
 */
@Component
@Slf4j
public class FailoverHandler {
    
    private final FailoverConfig config;
    
    /**
     * 执行故障转移
     * @param request 原始请求
     * @param failedModel 失败的模型
     * @param candidates 候选模型列表
     * @param exception 原始异常
     * @return 故障转移结果
     */
    public FailoverResult executeFailover(
            ChatRequest request,
            ModelInstance failedModel,
            List<ModelInstance> candidates,
            Exception exception) {
        
        log.warn("Model {} failed, executing failover. Error: {}", 
            failedModel.getConfig().getName(), exception.getMessage());
        
        // 标记模型进入冷却期
        failedModel.setHealthStatus(HealthStatus.COOLDOWN);
        failedModel.setCooldownUntil(
            Instant.now().plus(config.getCooldownDuration()));
        
        // 获取备用模型列表
        List<ModelInstance> backups = candidates.stream()
            .filter(m -> !m.getConfig().getName().equals(
                failedModel.getConfig().getName()))
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (backups.isEmpty()) {
            log.error("No backup models available for failover");
            return FailoverResult.failure("No backup available");
        }
        
        // 选择备用模型
        ModelInstance backup = backups.get(0);
        log.info("Failover to model: {}", backup.getConfig().getName());
        
        return FailoverResult.success(backup);
    }
}
```

```java
package io.mayfly.failover;

/**
 * 故障转移配置
 */
@Data
@ConfigurationProperties(prefix = "mayfly.failover")
public class FailoverConfig {
    
    /** 是否启用故障转移 */
    @Builder.Default
    private boolean enabled = true;
    
    /** 最大重试次数 */
    @Builder.Default
    private int maxRetries = 2;
    
    /** 冷却时间 */
    @Builder.Default
    private Duration cooldownDuration = Duration.ofSeconds(60);
    
    /** 可重试的异常类型 */
    @Builder.Default
    private List<String> retryableExceptions = Arrays.asList(
        "java.net.SocketTimeoutException",
        "org.springframework.web.client.HttpServerErrorException",
        "org.springframework.web.client.ResourceAccessException"
    );
}
```

---

### 3.5 mayfly-circuitbreaker（熔断限流模块）

```java
package io.mayfly.circuitbreaker;

/**
 * 熔断器管理器
 * 基于Resilience4j实现
 */
@Component
public class CircuitBreakerManager {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    
    /**
     * 获取模型对应的熔断器
     */
    public CircuitBreaker getCircuitBreaker(String modelName) {
        return circuitBreakerRegistry.circuitBreaker(modelName);
    }
    
    /**
     * 获取模型对应的限流器
     */
    public RateLimiter getRateLimiter(String modelName) {
        return rateLimiterRegistry.rateLimiter(modelName);
    }
    
    /**
     * 执行受保护的调用
     */
    public <T> T executeProtected(
            String modelName,
            Supplier<T> supplier) {
        
        CircuitBreaker cb = getCircuitBreaker(modelName);
        RateLimiter rl = getRateLimiter(modelName);
        
        return CircuitBreaker.decorateSupplier(cb,
            RateLimiter.decorateSupplier(rl, supplier))
            .get();
    }
}
```

---

### 3.6 mayfly-adapter（模型适配器模块）

```java
package io.mayfly.adapter;

/**
 * 模型适配器接口
 */
public interface ModelAdapter {
    
    /**
     * 创建ChatModel实例
     * @param config 模型配置
     * @return ChatModel实例
     */
    ChatModel createChatModel(ModelConfig config);
    
    /**
     * 支持的提供商
     */
    String getProvider();
}
```

```java
package io.mayfly.adapter.impl;

/**
 * 智谱AI适配器
 */
@Component
public class ZhipuModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        ZhipuAiApi zhipuAiApi = new ZhipuAiApi.Builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        
        return ZhipuAiChatModel.builder()
            .zhipuAiApi(zhipuAiApi)
            .defaultOptions(ZhipuAiChatOptions.builder()
                .model(config.getModel())
                .temperature(getProperty(config, "temperature", 0.7))
                .topP(getProperty(config, "top_p", 0.9))
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "zhipu";
    }
}
```

```java
package io.mayfly.adapter.impl;

/**
 * 通义千问适配器
 */
@Component
public class TongyiModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        
        return DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi)
            .defaultOptions(DashScopeChatOptions.builder()
                .model(config.getModel())
                .temperature(getProperty(config, "temperature", 0.7))
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "tongyi";
    }
}
```

```java
package io.mayfly.adapter.impl;

/**
 * DeepSeek适配器
 */
@Component
public class DeepSeekModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl() != null ? 
                config.getBaseUrl() : "https://api.deepseek.com")
            .build();
        
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(getProperty(config, "temperature", 0.7))
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "deepseek";
    }
}
```

---

### 3.7 mayfly-monitor（监控模块）

```java
package io.mayfly.monitor;

/**
 * 监控指标收集器
 * 基于Micrometer实现
 */
@Component
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // 计数器
    private final Counter totalCalls;
    private final Counter successCalls;
    private final Counter failureCalls;
    private final Counter failoverCalls;
    
    // 计时器
    private final Timer latencyTimer;
    
    // 分布摘要
    private final DistributionSummary inputTokens;
    private final DistributionSummary outputTokens;
    
    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.totalCalls = Counter.builder("mayfly.model.calls.total")
            .description("Total model calls")
            .register(meterRegistry);
        
        this.successCalls = Counter.builder("mayfly.model.calls.success")
            .description("Successful model calls")
            .register(meterRegistry);
        
        this.failureCalls = Counter.builder("mayfly.model.calls.failure")
            .description("Failed model calls")
            .register(meterRegistry);
        
        this.failoverCalls = Counter.builder("mayfly.model.calls.failover")
            .description("Failover calls")
            .register(meterRegistry);
        
        this.latencyTimer = Timer.builder("mayfly.model.latency.seconds")
            .description("Model call latency")
            .register(meterRegistry);
        
        this.inputTokens = DistributionSummary.builder("mayfly.model.tokens.input")
            .description("Input tokens")
            .register(meterRegistry);
        
        this.outputTokens = DistributionSummary.builder("mayfly.model.tokens.output")
            .description("Output tokens")
            .register(meterRegistry);
    }
    
    /**
     * 记录调用成功
     */
    public void recordSuccess(String modelName, long latencyMs, 
                              int inputTokens, int outputTokens) {
        totalCalls.increment();
        successCalls.increment();
        latencyTimer.record(Duration.ofMillis(latencyMs));
        this.inputTokens.record(inputTokens);
        this.outputTokens.record(outputTokens);
    }
    
    /**
     * 记录调用失败
     */
    public void recordFailure(String modelName, String errorType) {
        totalCalls.increment();
        failureCalls.increment();
    }
    
    /**
     * 记录故障转移
     */
    public void recordFailover(String fromModel, String toModel) {
        failoverCalls.increment();
    }
}
```

---

## 四、自动配置设计

### 4.1 Spring Boot Starter结构

```
mayfly-spring-boot-starter/
├── src/main/java/io/mayfly/autoconfigure/
│   ├── MayflyAutoConfiguration.java          # 主自动配置类
│   ├── MayflyProperties.java                 # 配置属性类
│   ├── ModelRegistryAutoConfiguration.java   # 模型注册中心配置
│   ├── RouterAutoConfiguration.java          # 路由策略配置
│   ├── LoadBalancerAutoConfiguration.java    # 负载均衡配置
│   ├── FailoverAutoConfiguration.java        # 故障转移配置
│   ├── CircuitBreakerAutoConfiguration.java  # 熔断器配置
│   └── MonitorAutoConfiguration.java         # 监控配置
├── src/main/resources/
│   ├── META-INF/spring.factories              # Spring Boot 2.x
│   ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports  # Spring Boot 3.x
│   └── META-INF/spring-configuration-metadata.json  # IDE提示元数据
```

### 4.2 配置属性类

```java
package io.mayfly.autoconfigure;

@Data
@ConfigurationProperties(prefix = "mayfly")
public class MayflyProperties {
    
    /** 是否启用Mayfly */
    @Builder.Default
    private boolean enabled = true;
    
    /** 模型配置列表 */
    private List<ModelConfig> models = new ArrayList<>();
    
    /** 路由配置 */
    private RouterConfig router = new RouterConfig();
    
    /** 故障转移配置 */
    private FailoverConfig failover = new FailoverConfig();
    
    /** 负载均衡配置 */
    private LoadBalancerConfig loadbalancer = new LoadBalancerConfig();
    
    /** 熔断器配置 */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    /** 限流器配置 */
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();
    
    /** 监控配置 */
    private MonitorConfig monitor = new MonitorConfig();
    
    @Data
    public static class RouterConfig {
        /** 路由策略 (fixed, weighted, rule-based) */
        @Builder.Default
        private String strategy = "fixed";
        
        /** 路由规则列表 */
        private List<RouterRule> rules = new ArrayList<>();
    }
    
    @Data
    public static class LoadBalancerConfig {
        /** 负载均衡策略 (round-robin, weighted-round-robin, random) */
        @Builder.Default
        private String strategy = "round-robin";
        
        /** 健康检查配置 */
        private HealthCheckConfig healthCheck = new HealthCheckConfig();
    }
    
    @Data
    public static class HealthCheckConfig {
        @Builder.Default
        private boolean enabled = true;
        
        @Builder.Default
        private Duration interval = Duration.ofSeconds(30);
        
        @Builder.Default
        private Duration timeout = Duration.ofSeconds(5);
        
        @Builder.Default
        private int unhealthyThreshold = 3;
    }
}
```

### 4.3 主自动配置类

```java
package io.mayfly.autoconfigure;

@Configuration
@EnableConfigurationProperties(MayflyProperties.class)
@ConditionalOnProperty(prefix = "mayfly", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfiguration(after = {SpringAIAutoConfiguration.class})
public class MayflyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry(MayflyProperties properties,
                                       ObjectProvider<List<ModelAdapter>> adapters) {
        return new DefaultModelRegistry(properties, adapters.getIfAvailable());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRouter modelRouter(ModelRegistry modelRegistry,
                                   RouterStrategy routerStrategy,
                                   FailoverHandler failoverHandler,
                                   CircuitBreakerManager circuitBreakerManager,
                                   MetricsCollector metricsCollector) {
        return new DefaultModelRouter(modelRegistry, routerStrategy, 
            failoverHandler, circuitBreakerManager, metricsCollector);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RouterStrategy routerStrategy(MayflyProperties properties,
                                         ObjectProvider<List<RouterStrategy>> strategies) {
        // 根据配置选择路由策略
        String strategyName = properties.getRouter().getStrategy();
        return strategies.getIfAvailable().stream()
            .filter(s -> s.getName().equals(strategyName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown router strategy: " + strategyName));
    }
    
    @Bean
    @ConditionalOnMissingBean
    public FailoverHandler failoverHandler(MayflyProperties properties) {
        return new FailoverHandler(properties.getFailover());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerManager circuitBreakerManager(MayflyProperties properties) {
        return new CircuitBreakerManager(properties.getCircuitBreaker(), 
            properties.getRateLimiter());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector(MeterRegistry meterRegistry,
                                             MayflyProperties properties) {
        if (properties.getMonitor().isEnabled()) {
            return new MetricsCollector(meterRegistry);
        }
        return new NoOpMetricsCollector();
    }
}
```

---

## 五、项目结构

### 5.1 完整目录结构

```
mayfly/
├── pom.xml                                    # 父POM
├── mayfly-core/                               # 核心模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/core/
│       ├── ModelRouter.java
│       ├── ModelRegistry.java
│       ├── ModelConfig.java
│       ├── ModelInstance.java
│       └── HealthStatus.java
├── mayfly-router/                             # 路由策略模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/router/
│       ├── RouterStrategy.java
│       ├── RouterRule.java
│       └── impl/
│           ├── FixedRouterStrategy.java
│           ├── WeightedRouterStrategy.java
│           └── RuleBasedRouterStrategy.java
├── mayfly-loadbalancer/                       # 负载均衡模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/loadbalancer/
│       ├── LoadBalancer.java
│       └── impl/
│           ├── RoundRobinLoadBalancer.java
│           └── WeightedRoundRobinLoadBalancer.java
├── mayfly-failover/                           # 故障转移模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/failover/
│       ├── FailoverHandler.java
│       ├── FailoverConfig.java
│       └── FailoverResult.java
├── mayfly-circuitbreaker/                     # 熔断限流模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/circuitbreaker/
│       ├── CircuitBreakerManager.java
│       └── RateLimiterManager.java
├── mayfly-adapter/                            # 模型适配器模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/adapter/
│       ├── ModelAdapter.java
│       └── impl/
│           ├── ZhipuModelAdapter.java
│           ├── TongyiModelAdapter.java
│           └── DeepSeekModelAdapter.java
├── mayfly-monitor/                            # 监控模块
│   ├── pom.xml
│   └── src/main/java/io/mayfly/monitor/
│       ├── MetricsCollector.java
│       └── NoOpMetricsCollector.java
├── mayfly-spring-boot-starter/                # Spring Boot Starter
│   ├── pom.xml
│   └── src/main/java/io/mayfly/autoconfigure/
│       ├── MayflyAutoConfiguration.java
│       ├── MayflyProperties.java
│       └── ...
└── mayfly-test/                               # 测试模块
    ├── pom.xml
    └── src/test/java/io/mayfly/
```

### 5.2 父POM设计

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>io.mayfly</groupId>
    <artifactId>mayfly-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>Mayfly Parent</name>
    <description>Enterprise Model Router for Spring AI</description>
    
    <modules>
        <module>mayfly-core</module>
        <module>mayfly-router</module>
        <module>mayfly-loadbalancer</module>
        <module>mayfly-failover</module>
        <module>mayfly-circuitbreaker</module>
        <module>mayfly-adapter</module>
        <module>mayfly-monitor</module>
        <module>mayfly-spring-boot-starter</module>
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.4</spring-boot.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
        <resilience4j.version>2.1.0</resilience4j.version>
        <micrometer.version>1.12.4</micrometer.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring AI BOM -->
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## 六、配置示例

### 6.1 最小配置

```yaml
mayfly:
  models:
    - name: zhipu-primary
      provider: zhipu
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
```

### 6.2 完整配置

```yaml
mayfly:
  enabled: true
  
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
      timeout: 30000
      tags:
        - backup
        - chinese
    
    - name: deepseek-coder
      provider: deepseek
      api-key: ${DEEPSEEK_API_KEY}
      model: deepseek-coder
      weight: 50
      tags:
        - code
  
  router:
    strategy: rule-based
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
  
  loadbalancer:
    strategy: weighted-round-robin
    health-check:
      enabled: true
      interval: 30s
      timeout: 5s
      unhealthy-threshold: 3
  
  failover:
    enabled: true
    max-retries: 2
    cooldown-duration: 60s
    retryable-exceptions:
      - java.net.SocketTimeoutException
      - org.springframework.web.client.HttpServerErrorException
  
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
    timeout-duration: 0s
  
  monitor:
    enabled: true
```

---

## 七、调用流程

### 7.1 正常调用流程

```
用户调用 ModelRouter.chat(request)
        ↓
    获取候选模型列表 (ModelRegistry)
        ↓
    执行路由策略 (RouterStrategy)
        ↓
    选择目标模型 (LoadBalancer)
        ↓
    检查熔断器状态 (CircuitBreaker)
        ↓
    检查限流器状态 (RateLimiter)
        ↓
    调用模型 (ModelAdapter → Spring AI)
        ↓
    记录监控指标 (MetricsCollector)
        ↓
    返回响应
```

### 7.2 故障转移流程

```
模型调用失败
        ↓
    捕获异常，判断是否可重试
        ↓
    记录失败指标
        ↓
    标记模型进入冷却期
        ↓
    获取备用模型列表
        ↓
    选择备用模型
        ↓
    重试调用
        ↓
    成功 → 返回响应
    失败 → 继续故障转移或返回错误
```

---

## 八、扩展点设计

### 8.1 SPI扩展机制

```java
package io.mayfly.spi;

/**
 * 路由策略SPI
 * 用户可通过实现此接口自定义路由策略
 */
public interface RouterStrategyProvider {
    
    /**
     * 获取路由策略
     */
    RouterStrategy getRouterStrategy();
}
```

```java
package io.mayfly.spi;

/**
 * 模型适配器SPI
 * 用户可通过实现此接口支持新的模型提供商
 */
public interface ModelAdapterProvider {
    
    /**
     * 获取模型适配器
     */
    ModelAdapter getModelAdapter();
}
```

### 8.2 自定义扩展示例

```java
// 自定义路由策略
@Component
public class CustomRouterStrategy implements RouterStrategy {
    
    @Override
    public ModelInstance select(ChatRequest request, 
                                List<ModelInstance> candidates) {
        // 自定义路由逻辑
        // 例如：基于用户地理位置路由
        String region = request.getMetadata().get("region");
        return candidates.stream()
            .filter(m -> m.getConfig().getTags().contains(region))
            .findFirst()
            .orElse(candidates.get(0));
    }
    
    @Override
    public String getName() {
        return "custom-region-based";
    }
}
```

---

## 九、性能设计

### 9.1 性能优化策略

| 优化点 | 策略 | 预期效果 |
|--------|------|---------|
| 路由延迟 | 缓存模型列表，避免重复查询 | < 1ms |
| 并发安全 | 使用AtomicInteger等无锁数据结构 | 高并发安全 |
| 连接复用 | 复用HTTP连接池 | 减少连接开销 |
| 异步支持 | 支持WebFlux异步调用 | 提高吞吐量 |

### 9.2 性能指标

| 指标 | 目标值 | 测试条件 |
|------|--------|---------|
| 路由决策延迟 | < 1ms | 10个模型 |
| 吞吐量 | > 1000 QPS | 单实例 |
| 内存占用 | < 50MB | 基础配置 |
| 启动时间增加 | < 3s | Spring Boot启动 |

---

## 十、安全设计

### 10.1 API Key管理

```yaml
# 推荐方式1：环境变量
mayfly:
  models:
    - api-key: ${ZHIPU_API_KEY}

# 推荐方式2：配置中心
mayfly:
  models:
    - api-key: ${config-center:mayfly.models[0].api-key}

# 推荐方式3：Vault
mayfly:
  models:
    - api-key: ${vault:secret/mayfly#zhipu-api-key}
```

### 10.2 敏感信息脱敏

```java
@Component
public class LogSanitizer {
    
    /**
     * 脱敏API Key
     */
    public String sanitizeApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + 
               apiKey.substring(apiKey.length() - 4);
    }
}
```

---

## 十一、部署架构

### 11.1 应用内嵌模式（主要模式）

```
┌─────────────────────────────────────┐
│         用户Spring Boot应用          │
│  ┌───────────────────────────────┐  │
│  │        Mayfly Jar包            │  │
│  │  ┌─────────┬─────────┬──────┐ │  │
│  │  │ 路由层   │ 负载均衡 │ 熔断  │ │  │
│  │  └─────────┴─────────┴──────┘ │  │
│  └───────────────────────────────┘  │
└──────────────┬──────────────────────┘
               │
    ┌──────────┼──────────┐
    ↓          ↓          ↓
  智谱AI    通义千问   DeepSeek
```

### 11.2 依赖关系

```
用户应用
  └── mayfly-spring-boot-starter
        ├── mayfly-core
        ├── mayfly-router
        ├── mayfly-loadbalancer
        ├── mayfly-failover
        ├── mayfly-circuitbreaker
        │     └── resilience4j
        ├── mayfly-adapter
        │     └── spring-ai
        └── mayfly-monitor
              └── micrometer
```

---

## 十二、测试策略

### 12.1 测试分层

| 测试类型 | 覆盖范围 | 工具 |
|---------|---------|------|
| 单元测试 | 核心逻辑 | JUnit 5 + Mockito |
| 集成测试 | Spring Boot集成 | Spring Boot Test |
| 端到端测试 | 完整调用链路 | Testcontainers |
| 性能测试 | 路由性能 | JMH |

### 12.2 Mock策略

```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public ChatModel mockChatModel() {
        return Mockito.mock(ChatModel.class);
    }
}
```

---

## 十三、版本兼容

### 13.1 兼容性矩阵

| Mayfly版本 | Spring Boot | Spring AI | Java |
|-----------|-------------|-----------|------|
| 1.0.x | 3.2.x | 1.0.0-M6+ | 17+ |
| 1.1.x | 3.3.x | 1.0.0+ | 17+ |

### 13.2 升级策略

1. **小版本升级**（1.0.0 → 1.0.1）：向后兼容，直接升级
2. **中版本升级**（1.0.x → 1.1.x）：可能有配置变更，查看迁移指南
3. **大版本升级**（1.x → 2.x）：可能有Breaking Changes，需要代码调整

---

**文档审批**：

| 角色 | 姓名 | 日期 | 意见 |
|------|------|------|------|
| 技术负责人 | - | - | - |
| 架构师 | - | - | - |

**文档变更记录**：

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| v1.0 | 2026-04-15 | 初始版本 | 系统架构设计Agent |
