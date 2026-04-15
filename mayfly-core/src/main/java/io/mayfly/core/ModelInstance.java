package io.mayfly.core;

import lombok.Data;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    
    public ModelInstance(ModelConfig config, ChatModel chatModel) {
        this.config = config;
        this.chatModel = chatModel;
    }
    
    /**
     * 记录请求成功
     */
    public void recordSuccess(long latency) {
        totalRequests.incrementAndGet();
        if (activeRequests.get() > 0) {
            activeRequests.decrementAndGet();
        }
        long currentAvg = avgLatency.get();
        avgLatency.set((long) (currentAvg * 0.9 + latency * 0.1));
    }
    
    /**
     * 记录请求失败
     */
    public void recordFailure() {
        failedRequests.incrementAndGet();
        if (activeRequests.get() > 0) {
            activeRequests.decrementAndGet();
        }
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
    
    /**
     * 获取失败率
     */
    public double getFailureRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failedRequests.get() / total;
    }
}
