package io.mayfly.autoconfigure;

import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelRegistry;
import io.mayfly.core.ModelRouter;
import io.mayfly.failover.FailoverHandler;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.monitor.MicrometerMetricsCollector;
import io.mayfly.monitor.NoOpMetricsCollector;
import io.mayfly.router.RouterStrategy;
import io.mayfly.router.impl.FixedRouterStrategy;
import io.mayfly.router.impl.RuleBasedRouterStrategy;
import io.mayfly.router.impl.WeightedRouterStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mayfly自动配置单元测试
 */
@DisplayName("Mayfly自动配置测试")
class MayflyAutoConfigurationTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MayflyAutoConfiguration.class));
    
    @Nested
    @DisplayName("基础配置测试")
    class BasicConfigurationTests {
        
        @Test
        @DisplayName("默认配置 - 启用Mayfly")
        void testDefaultConfiguration_Enabled() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(MayflyAutoConfiguration.class);
                assertThat(context).hasSingleBean(CircuitBreakerManager.class);
                assertThat(context).hasSingleBean(FailoverHandler.class);
                assertThat(context).hasSingleBean(MetricsCollector.class);
            });
        }
        
        @Test
        @DisplayName("禁用Mayfly - 不创建Bean")
        void testDisabledMayfly_NoBeans() {
            contextRunner
                .withPropertyValues("mayfly.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MayflyAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(CircuitBreakerManager.class);
                    assertThat(context).doesNotHaveBean(FailoverHandler.class);
                });
        }
    }
    
    @Nested
    @DisplayName("监控配置测试")
    class MonitorConfigurationTests {
        
        @Test
        @DisplayName("启用监控 - 返回MicrometerMetricsCollector")
        void testMonitorEnabled_MicrometerMetricsCollector() {
            contextRunner
                .withPropertyValues("mayfly.monitor.enabled=true")
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    MetricsCollector collector = context.getBean(MetricsCollector.class);
                    assertThat(collector).isInstanceOf(MicrometerMetricsCollector.class);
                });
        }
        
        @Test
        @DisplayName("禁用监控 - 返回NoOpMetricsCollector")
        void testMonitorDisabled_NoOpMetricsCollector() {
            contextRunner
                .withPropertyValues("mayfly.monitor.enabled=false")
                .run(context -> {
                    MetricsCollector collector = context.getBean(MetricsCollector.class);
                    assertThat(collector).isInstanceOf(NoOpMetricsCollector.class);
                });
        }
        
        @Test
        @DisplayName("启用监控但无MeterRegistry - 返回NoOpMetricsCollector")
        void testMonitorEnabled_NoMeterRegistry_NoOpMetricsCollector() {
            contextRunner
                .withPropertyValues("mayfly.monitor.enabled=true")
                .run(context -> {
                    MetricsCollector collector = context.getBean(MetricsCollector.class);
                    assertThat(collector).isInstanceOf(NoOpMetricsCollector.class);
                });
        }
    }
    
    @Nested
    @DisplayName("路由策略配置测试")
    class RouterStrategyTests {
        
        @Test
        @DisplayName("固定路由策略 - 默认")
        void testFixedRouterStrategy_Default() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(FixedRouterStrategy.class);
                assertThat(context).doesNotHaveBean(WeightedRouterStrategy.class);
                assertThat(context).doesNotHaveBean(RuleBasedRouterStrategy.class);
                
                RouterStrategy strategy = context.getBean(RouterStrategy.class);
                assertThat(strategy).isInstanceOf(FixedRouterStrategy.class);
            });
        }
        
        @Test
        @DisplayName("加权路由策略")
        void testWeightedRouterStrategy() {
            contextRunner
                .withPropertyValues("mayfly.router.strategy=weighted")
                .run(context -> {
                    assertThat(context).hasSingleBean(WeightedRouterStrategy.class);
                    assertThat(context).doesNotHaveBean(FixedRouterStrategy.class);
                    assertThat(context).doesNotHaveBean(RuleBasedRouterStrategy.class);
                    
                    RouterStrategy strategy = context.getBean(RouterStrategy.class);
                    assertThat(strategy).isInstanceOf(WeightedRouterStrategy.class);
                });
        }
        
        @Test
        @DisplayName("规则路由策略")
        void testRuleBasedRouterStrategy() {
            contextRunner
                .withPropertyValues("mayfly.router.strategy=rule-based")
                .run(context -> {
                    assertThat(context).hasSingleBean(RuleBasedRouterStrategy.class);
                    assertThat(context).doesNotHaveBean(FixedRouterStrategy.class);
                    assertThat(context).doesNotHaveBean(WeightedRouterStrategy.class);
                    
                    RouterStrategy strategy = context.getBean(RouterStrategy.class);
                    assertThat(strategy).isInstanceOf(RuleBasedRouterStrategy.class);
                });
        }
    }
    
    @Nested
    @DisplayName("组件Bean测试")
    class ComponentBeanTests {
        
        @Test
        @DisplayName("CircuitBreakerManager - 默认配置")
        void testCircuitBreakerManager_DefaultConfig() {
            contextRunner.run(context -> {
                CircuitBreakerManager manager = context.getBean(CircuitBreakerManager.class);
                assertThat(manager).isNotNull();
                
                io.github.resilience4j.circuitbreaker.CircuitBreaker cb = 
                    manager.getCircuitBreaker("test-model");
                assertThat(cb).isNotNull();
                assertThat(cb.getName()).isEqualTo("test-model");
            });
        }
        
        @Test
        @DisplayName("FailoverHandler - 默认配置")
        void testFailoverHandler_DefaultConfig() {
            contextRunner.run(context -> {
                FailoverHandler handler = context.getBean(FailoverHandler.class);
                assertThat(handler).isNotNull();
            });
        }
    }
    
    @Nested
    @DisplayName("自定义Bean覆盖测试")
    class CustomBeanOverrideTests {
        
        @Test
        @DisplayName("自定义MetricsCollector - 覆盖默认")
        void testCustomMetricsCollector_OverridesDefault() {
            MetricsCollector customCollector = new NoOpMetricsCollector();
            
            contextRunner
                .withBean("customMetricsCollector", MetricsCollector.class, () -> customCollector)
                .run(context -> {
                    MetricsCollector collector = context.getBean(MetricsCollector.class);
                    assertThat(collector).isSameAs(customCollector);
                });
        }
        
        @Test
        @DisplayName("自定义CircuitBreakerManager - 覆盖默认")
        void testCustomCircuitBreakerManager_OverridesDefault() {
            CircuitBreakerManager customManager = new CircuitBreakerManager();
            
            contextRunner
                .withBean("customCircuitBreakerManager", CircuitBreakerManager.class, () -> customManager)
                .run(context -> {
                    CircuitBreakerManager manager = context.getBean(CircuitBreakerManager.class);
                    assertThat(manager).isSameAs(customManager);
                });
        }
    }
}
