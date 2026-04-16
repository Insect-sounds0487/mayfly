package io.mayfly.demo.controller;

import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelRegistry;
import io.mayfly.core.ModelRouter;
import io.mayfly.failover.FailoverHandler;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业级特性测试控制器
 * 用于测试 Mayfly 的熔断、重试、负载均衡等功能
 */
@RestController
@RequestMapping("/api/test")
public class EnterpriseFeatureTestController {
    
    private final ModelRouter modelRouter;
    private final ModelRegistry modelRegistry;
    private final CircuitBreakerManager circuitBreakerManager;
    private final FailoverHandler failoverHandler;
    
    public EnterpriseFeatureTestController(
            ModelRouter modelRouter,
            ModelRegistry modelRegistry,
            CircuitBreakerManager circuitBreakerManager,
            FailoverHandler failoverHandler) {
        this.modelRouter = modelRouter;
        this.modelRegistry = modelRegistry;
        this.circuitBreakerManager = circuitBreakerManager;
        this.failoverHandler = failoverHandler;
    }
    
    /**
     * 测试负载均衡
     * 连续发送多个请求，观察请求被分配到哪个模型
     */
    @PostMapping("/load-balancer")
    public Map<String, Object> testLoadBalancer(@RequestParam(defaultValue = "5") int count) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Prompt prompt = new Prompt(
                List.of(new UserMessage("这是第 " + (i + 1) + " 个请求")),
                null
            );
            
            ChatResponse response = modelRouter.chat(prompt);
            String modelName = response.getMetadata() != null 
                ? response.getMetadata().getModel() 
                : "unknown";
            
            Map<String, Object> result = new HashMap<>();
            result.put("requestNumber", i + 1);
            result.put("model", modelName);
            result.put("success", true);
            results.add(result);
        }
        
        return Map.of(
            "test", "load-balancer",
            "totalRequests", count,
            "results", results,
            "description", "观察请求如何分配到不同的模型实例"
        );
    }
    
    /**
     * 查看当前注册的模型
     */
    @GetMapping("/models")
    public Map<String, Object> getModels() {
        var models = modelRegistry.getAllModels();
        
        return Map.of(
            "totalModels", models.size(),
            "models", models.stream().map(m -> Map.of(
                "name", m.getConfig().getName(),
                "provider", m.getConfig().getProvider(),
                "model", m.getConfig().getModel(),
                "weight", m.getConfig().getWeight(),
                "available", m.isAvailable()
            )).toList(),
            "description", "当前注册的所有模型"
        );
    }
    
    /**
     * 查看熔断器状态
     */
    @GetMapping("/circuit-breaker/status")
    public Map<String, Object> getCircuitBreakerStatus() {
        return Map.of(
            "circuitBreakerEnabled", circuitBreakerManager != null,
            "description", "熔断器当前状态",
            "note", "当模型连续失败时，熔断器会自动打开，阻止后续请求"
        );
    }
    
    /**
     * 测试重试机制
     * 模拟一个会失败的请求，观察 Mayfly 如何重试
     */
    @PostMapping("/retry")
    public Map<String, Object> testRetry() {
        long startTime = System.currentTimeMillis();
        
        Prompt prompt = new Prompt(
            List.of(new UserMessage("测试重试机制")),
            null
        );
        
        try {
            ChatResponse response = modelRouter.chat(prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            return Map.of(
                "test", "retry",
                "success", true,
                "model", response.getMetadata() != null ? response.getMetadata().getModel() : "unknown",
                "durationMs", duration,
                "description", "如果第一次请求失败，Mayfly 会自动重试（配置中 max-retries: 2）"
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return Map.of(
                "test", "retry",
                "success", false,
                "error", e.getMessage(),
                "durationMs", duration,
                "description", "重试次数用尽后仍然失败"
            );
        }
    }
    
    /**
     * 压力测试 - 快速发送大量请求
     * 用于观察熔断器是否会在失败率过高时触发
     */
    @PostMapping("/stress-test")
    public Map<String, Object> stressTest(@RequestParam(defaultValue = "20") int count) {
        int successCount = 0;
        int failureCount = 0;
        List<String> models = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            try {
                Prompt prompt = new Prompt(
                    List.of(new UserMessage("压力测试请求 " + (i + 1))),
                    null
                );
                
                ChatResponse response = modelRouter.chat(prompt);
                successCount++;
                
                if (response.getMetadata() != null) {
                    models.add(response.getMetadata().getModel());
                }
            } catch (Exception e) {
                failureCount++;
            }
        }
        
        return Map.of(
            "test", "stress-test",
            "totalRequests", count,
            "successCount", successCount,
            "failureCount", failureCount,
            "successRate", String.format("%.2f%%", (successCount * 100.0 / count)),
            "modelsUsed", models,
            "description", "观察在高并发情况下的成功率和模型使用情况"
        );
    }
    
    /**
     * 获取 Mayfly 配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
            "features", Map.of(
                "loadBalancer", "加权轮询 (weighted-round-robin)",
                "router", "权重路由 (weighted)",
                "failover", "已启用 (enabled: true)",
                "circuitBreaker", "已启用 (enabled: true)",
                "retry", "已启用 (max-retries: 2)"
            ),
            "description", "Mayfly 当前配置的企业级特性"
        );
    }
}
