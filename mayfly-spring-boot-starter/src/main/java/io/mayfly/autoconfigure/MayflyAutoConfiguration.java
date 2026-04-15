package io.mayfly.autoconfigure;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.circuitbreaker.CircuitBreakerConfigProperties;
import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.core.DefaultModelRegistry;
import io.mayfly.core.DefaultModelRouter;
import io.mayfly.core.ModelRegistry;
import io.mayfly.core.ModelRouter;
import io.mayfly.failover.FailoverHandler;
import io.mayfly.loadbalancer.LoadBalancer;
import io.mayfly.loadbalancer.impl.RoundRobinLoadBalancer;
import io.mayfly.loadbalancer.impl.WeightedRoundRobinLoadBalancer;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.router.RouterStrategy;
import io.mayfly.router.impl.FixedRouterStrategy;
import io.mayfly.router.impl.RuleBasedRouterStrategy;
import io.mayfly.router.impl.WeightedRouterStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Mayfly自动配置类
 */
@Configuration
@EnableConfigurationProperties(MayflyProperties.class)
@ConditionalOnProperty(prefix = "mayfly", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MayflyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public FixedRouterStrategy fixedRouterStrategy() {
        return new FixedRouterStrategy();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public WeightedRouterStrategy weightedRouterStrategy() {
        return new WeightedRouterStrategy();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RuleBasedRouterStrategy ruleBasedRouterStrategy() {
        return new RuleBasedRouterStrategy();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RoundRobinLoadBalancer roundRobinLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public WeightedRoundRobinLoadBalancer weightedRoundRobinLoadBalancer() {
        return new WeightedRoundRobinLoadBalancer();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry(MayflyProperties properties,
                                       ObjectProvider<List<ModelAdapter>> adapters) {
        return new DefaultModelRegistry(properties, adapters.getIfAvailable());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RouterStrategy routerStrategy(MayflyProperties properties,
                                         FixedRouterStrategy fixedRouterStrategy,
                                         WeightedRouterStrategy weightedRouterStrategy,
                                         RuleBasedRouterStrategy ruleBasedRouterStrategy) {
        String strategyName = properties.getRouter().getStrategy();
        
        if (properties.getRouter().getRules() != null && !properties.getRouter().getRules().isEmpty()) {
            ruleBasedRouterStrategy.setRules(properties.getRouter().getRules());
        }
        
        switch (strategyName) {
            case "fixed":
                return fixedRouterStrategy;
            case "weighted":
                return weightedRouterStrategy;
            case "rule-based":
                return ruleBasedRouterStrategy;
            default:
                return fixedRouterStrategy;
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer(MayflyProperties properties,
                                     RoundRobinLoadBalancer roundRobinLoadBalancer,
                                     WeightedRoundRobinLoadBalancer weightedRoundRobinLoadBalancer) {
        String strategyName = properties.getLoadbalancer().getStrategy();
        
        switch (strategyName) {
            case "round-robin":
                return roundRobinLoadBalancer;
            case "weighted-round-robin":
                return weightedRoundRobinLoadBalancer;
            default:
                return roundRobinLoadBalancer;
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    public FailoverHandler failoverHandler(MayflyProperties properties) {
        io.mayfly.failover.FailoverConfig config = io.mayfly.failover.FailoverConfig.builder()
            .enabled(properties.getFailover().isEnabled())
            .maxRetries(properties.getFailover().getMaxRetries())
            .cooldownDuration(properties.getFailover().getCooldownDuration())
            .retryableExceptions(properties.getFailover().getRetryableExceptions())
            .build();
        return new FailoverHandler(config);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerManager circuitBreakerManager(MayflyProperties properties) {
        CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
            .failureRateThreshold(properties.getCircuitBreaker().getFailureRateThreshold())
            .waitDurationInOpenState(properties.getCircuitBreaker().getWaitDurationInOpenState())
            .slidingWindowSize(properties.getCircuitBreaker().getSlidingWindowSize())
            .minimumNumberOfCalls(properties.getCircuitBreaker().getMinimumNumberOfCalls())
            .limitRefreshPeriod(properties.getRateLimiter().getLimitRefreshPeriod())
            .limitForPeriod(properties.getRateLimiter().getLimitForPeriod())
            .timeoutDuration(properties.getRateLimiter().getTimeoutDuration())
            .build();
        return new CircuitBreakerManager(config);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRouter modelRouter(ModelRegistry modelRegistry,
                                   RouterStrategy routerStrategy,
                                   FailoverHandler failoverHandler,
                                   CircuitBreakerManager circuitBreakerManager,
                                   ObjectProvider<MetricsCollector> metricsCollectorProvider) {
        return new DefaultModelRouter(modelRegistry, routerStrategy, 
            failoverHandler, circuitBreakerManager, metricsCollectorProvider.getIfAvailable());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector(ObjectProvider<MeterRegistry> meterRegistryProvider,
                                             MayflyProperties properties) {
        if (properties.getMonitor().isEnabled() && meterRegistryProvider.getIfAvailable() != null) {
            return new MetricsCollector(meterRegistryProvider.getIfAvailable());
        }
        return null;
    }
}
