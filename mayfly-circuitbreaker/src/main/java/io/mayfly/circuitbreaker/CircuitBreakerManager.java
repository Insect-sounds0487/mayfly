package io.mayfly.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 熔断器管理器
 * 基于Resilience4j实现
 */
@Component
public class CircuitBreakerManager {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public CircuitBreakerManager() {
        this(CircuitBreakerConfigProperties.builder().build());
    }
    
    public CircuitBreakerManager(CircuitBreakerConfigProperties config) {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.getFailureRateThreshold())
            .waitDurationInOpenState(config.getWaitDurationInOpenState())
            .slidingWindowSize(config.getSlidingWindowSize())
            .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
            .build();
        
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(config.getLimitRefreshPeriod())
            .limitForPeriod(config.getLimitForPeriod())
            .timeoutDuration(config.getTimeoutDuration())
            .build();
        
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);
        this.rateLimiterRegistry = RateLimiterRegistry.of(rlConfig);
    }
    
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
    public <T> T executeProtected(String modelName, Supplier<T> supplier) {
        CircuitBreaker cb = getCircuitBreaker(modelName);
        RateLimiter rl = getRateLimiter(modelName);
        
        return CircuitBreaker.decorateSupplier(cb,
            RateLimiter.decorateSupplier(rl, supplier))
            .get();
    }
}
