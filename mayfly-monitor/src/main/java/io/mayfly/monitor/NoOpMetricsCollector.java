package io.mayfly.monitor;

/**
 * 空操作监控收集器
 * 当监控未启用时使用
 */
public class NoOpMetricsCollector {
    
    public void recordSuccess(String modelName, long latencyMs, 
                              int inputTokens, int outputTokens) {
        // No-op
    }
    
    public void recordFailure(String modelName, String errorType) {
        // No-op
    }
    
    public void recordFailover(String fromModel, String toModel) {
        // No-op
    }
}
