package io.mayfly.autoconfigure;

import io.mayfly.router.RouterRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Mayfly配置属性类
 */
@Data
@ConfigurationProperties(prefix = "mayfly")
public class MayflyProperties {
    
    /** 是否启用Mayfly */
    private boolean enabled = true;
    
    /** 模型配置列表 */
    private List<io.mayfly.core.ModelConfig> models = new ArrayList<>();
    
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
        private String strategy = "fixed";
        
        /** 路由规则列表 */
        private List<RouterRule> rules = new ArrayList<>();
    }
    
    @Data
    public static class FailoverConfig {
        private boolean enabled = true;
        private int maxRetries = 2;
        private Duration cooldownDuration = Duration.ofSeconds(60);
        private List<String> retryableExceptions = new ArrayList<>();
    }
    
    @Data
    public static class LoadBalancerConfig {
        private String strategy = "round-robin";
        private HealthCheckConfig healthCheck = new HealthCheckConfig();
    }
    
    @Data
    public static class HealthCheckConfig {
        private boolean enabled = true;
        private Duration interval = Duration.ofSeconds(30);
        private Duration timeout = Duration.ofSeconds(5);
        private int unhealthyThreshold = 3;
    }
    
    @Data
    public static class CircuitBreakerConfig {
        private boolean enabled = true;
        private float failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
    }
    
    @Data
    public static class RateLimiterConfig {
        private boolean enabled = true;
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);
        private int limitForPeriod = 100;
        private Duration timeoutDuration = Duration.ZERO;
    }
    
    @Data
    public static class MonitorConfig {
        private boolean enabled = true;
    }
}
