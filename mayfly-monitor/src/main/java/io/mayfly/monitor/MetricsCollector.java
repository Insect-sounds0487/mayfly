package io.mayfly.monitor;

/**
 * 监控指标收集器接口
 */
public interface MetricsCollector {
    
    /**
     * 记录调用成功
     */
    void recordSuccess(String modelName, long latencyMs, int inputTokens, int outputTokens);
    
    /**
     * 记录调用失败
     */
    void recordFailure(String modelName, String errorType);
    
    /**
     * 记录故障转移
     */
    void recordFailover(String fromModel, String toModel);
}
