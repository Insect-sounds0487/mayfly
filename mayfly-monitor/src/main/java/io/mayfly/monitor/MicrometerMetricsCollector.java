package io.mayfly.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 监控指标收集器实现
 * 基于 Micrometer 实现
 */
@Component
public class MicrometerMetricsCollector implements MetricsCollector {
    
    private final Counter totalCalls;
    private final Counter successCalls;
    private final Counter failureCalls;
    private final Counter failoverCalls;
    private final Timer latencyTimer;
    private final DistributionSummary inputTokens;
    private final DistributionSummary outputTokens;
    
    public MicrometerMetricsCollector(MeterRegistry meterRegistry) {
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
    
    @Override
    public void recordSuccess(String modelName, long latencyMs, 
                              int inputTokens, int outputTokens) {
        totalCalls.increment();
        successCalls.increment();
        latencyTimer.record(Duration.ofMillis(latencyMs));
        this.inputTokens.record(inputTokens);
        this.outputTokens.record(outputTokens);
    }
    
    @Override
    public void recordFailure(String modelName, String errorType) {
        totalCalls.increment();
        failureCalls.increment();
    }
    
    @Override
    public void recordFailover(String fromModel, String toModel) {
        failoverCalls.increment();
    }
}
