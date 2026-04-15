package io.mayfly.circuitbreaker;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * 熔断器配置属性
 */
@Data
@Builder
public class CircuitBreakerConfigProperties {
    
    /** 失败率阈值（百分比） */
    @Builder.Default
    private float failureRateThreshold = 50;
    
    /** 熔断器打开后等待时间 */
    @Builder.Default
    private Duration waitDurationInOpenState = Duration.ofSeconds(60);
    
    /** 滑动窗口大小 */
    @Builder.Default
    private int slidingWindowSize = 10;
    
    /** 最小调用次数 */
    @Builder.Default
    private int minimumNumberOfCalls = 5;
    
    /** 限流周期 */
    @Builder.Default
    private Duration limitRefreshPeriod = Duration.ofSeconds(1);
    
    /** 限流周期内允许的调用次数 */
    @Builder.Default
    private int limitForPeriod = 100;
    
    /** 限流超时时间 */
    @Builder.Default
    private Duration timeoutDuration = Duration.ZERO;
}
