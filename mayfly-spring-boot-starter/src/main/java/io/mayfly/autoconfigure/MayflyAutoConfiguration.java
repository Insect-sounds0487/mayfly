package io.mayfly.autoconfigure;

import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.core.ModelRegistry;
import io.mayfly.core.ModelRouter;
import io.mayfly.failover.FailoverHandler;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.monitor.NoOpMetricsCollector;
import io.mayfly.router.RouterStrategy;
import io.mayfly.router.impl.FixedRouterStrategy;
import io.mayfly.router.impl.RuleBasedRouterStrategy;
import io.mayfly.router.impl.WeightedRouterStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Mayfly 自动配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "mayfly", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MayflyProperties.class)
public class MayflyAutoConfiguration {
    
    private final MayflyProperties properties;
    
    public MayflyAutoConfiguration(MayflyProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerManager circuitBreakerManager() {
        return new CircuitBreakerManager();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public FailoverHandler failoverHandler() {
        return new FailoverHandler();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector() {
        if (properties.getMonitor() != null && properties.getMonitor().isEnabled()) {
            return new NoOpMetricsCollector();
        }
        return new NoOpMetricsCollector();
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "fixedRouterStrategy")
    public FixedRouterStrategy fixedRouterStrategy() {
        return new FixedRouterStrategy();
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "weightedRouterStrategy")
    public WeightedRouterStrategy weightedRouterStrategy() {
        return new WeightedRouterStrategy();
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "ruleBasedRouterStrategy")
    public RuleBasedRouterStrategy ruleBasedRouterStrategy() {
        RuleBasedRouterStrategy strategy = new RuleBasedRouterStrategy();
        if (properties.getRouter() != null && properties.getRouter().getRules() != null) {
            strategy.setRules(properties.getRouter().getRules());
        }
        return strategy;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RouterStrategy routerStrategy(
            ObjectProvider<FixedRouterStrategy> fixedProvider,
            ObjectProvider<WeightedRouterStrategy> weightedProvider,
            ObjectProvider<RuleBasedRouterStrategy> ruleBasedProvider) {
        
        String strategy = properties.getRouter() != null 
            ? properties.getRouter().getStrategy() : "fixed";
        
        return switch (strategy) {
            case "weighted" -> weightedProvider.getIfAvailable();
            case "rule-based" -> ruleBasedProvider.getIfAvailable();
            default -> fixedProvider.getIfAvailable();
        };
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry(
            CircuitBreakerManager circuitBreakerManager,
            FailoverHandler failoverHandler,
            MetricsCollector metricsCollector,
            RouterStrategy routerStrategy,
            ObjectProvider<List<io.mayfly.adapter.ModelAdapter>> adaptersProvider) {
        
        List<io.mayfly.adapter.ModelAdapter> adapters = 
            adaptersProvider.getIfAvailable(() -> List.of());
        
        return new DefaultModelRegistry(properties, adapters);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRouter modelRouter(
            ModelRegistry modelRegistry,
            RouterStrategy routerStrategy,
            FailoverHandler failoverHandler,
            CircuitBreakerManager circuitBreakerManager,
            MetricsCollector metricsCollector) {
        
        return new DefaultModelRouter(
            modelRegistry,
            routerStrategy,
            failoverHandler,
            circuitBreakerManager,
            metricsCollector
        );
    }
}
