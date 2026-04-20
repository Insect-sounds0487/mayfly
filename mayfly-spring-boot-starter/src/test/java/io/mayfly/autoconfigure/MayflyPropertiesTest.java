package io.mayfly.autoconfigure;

import io.mayfly.core.ModelConfig;
import io.mayfly.router.RouterRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MayflyProperties 单元测试")
class MayflyPropertiesTest {
    
    private MayflyProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new MayflyProperties();
    }
    
    @Test
    @DisplayName("测试默认配置值")
    void testDefaultValues() {
        assertTrue(properties.isEnabled());
        assertNotNull(properties.getModels());
        assertTrue(properties.getModels().isEmpty());
        assertNotNull(properties.getRouter());
        assertNotNull(properties.getFailover());
        assertNotNull(properties.getLoadbalancer());
        assertNotNull(properties.getCircuitBreaker());
        assertNotNull(properties.getRateLimiter());
        assertNotNull(properties.getMonitor());
    }
    
    @Nested
    @DisplayName("RouterConfig 测试")
    class RouterConfigTests {
        
        @Test
        @DisplayName("测试路由配置默认值")
        void testRouterDefaultValues() {
            MayflyProperties.RouterConfig router = new MayflyProperties.RouterConfig();
            
            assertEquals("fixed", router.getStrategy());
            assertNotNull(router.getRules());
            assertTrue(router.getRules().isEmpty());
        }
        
        @Test
        @DisplayName("测试自定义路由策略")
        void testCustomRouterStrategy() {
            MayflyProperties.RouterConfig router = new MayflyProperties.RouterConfig();
            router.setStrategy("weighted");
            
            assertEquals("weighted", router.getStrategy());
        }
        
        @Test
        @DisplayName("测试添加路由规则")
        void testAddRouterRules() {
            MayflyProperties.RouterConfig router = new MayflyProperties.RouterConfig();
            
            RouterRule rule1 = RouterRule.builder()
                .name("rule-1")
                .condition("true")
                .targetModel("model-1")
                .priority(1)
                .build();
            
            RouterRule rule2 = RouterRule.builder()
                .name("rule-2")
                .condition("false")
                .targetModel("model-2")
                .priority(2)
                .build();
            
            router.getRules().add(rule1);
            router.getRules().add(rule2);
            
            assertEquals(2, router.getRules().size());
            assertEquals("rule-1", router.getRules().get(0).getName());
            assertEquals("rule-2", router.getRules().get(1).getName());
        }
    }
    
    @Nested
    @DisplayName("FailoverConfig 测试")
    class FailoverConfigTests {
        
        @Test
        @DisplayName("测试故障转移配置默认值")
        void testFailoverDefaultValues() {
            MayflyProperties.FailoverConfig failover = new MayflyProperties.FailoverConfig();
            
            assertTrue(failover.isEnabled());
            assertEquals(2, failover.getMaxRetries());
            assertEquals(Duration.ofSeconds(60), failover.getCooldownDuration());
            assertNotNull(failover.getRetryableExceptions());
            assertTrue(failover.getRetryableExceptions().isEmpty());
        }
        
        @Test
        @DisplayName("测试自定义故障转移配置")
        void testCustomFailoverConfig() {
            MayflyProperties.FailoverConfig failover = new MayflyProperties.FailoverConfig();
            
            failover.setEnabled(false);
            failover.setMaxRetries(5);
            failover.setCooldownDuration(Duration.ofMinutes(5));
            failover.setRetryableExceptions(Arrays.asList("java.io.IOException"));
            
            assertFalse(failover.isEnabled());
            assertEquals(5, failover.getMaxRetries());
            assertEquals(Duration.ofMinutes(5), failover.getCooldownDuration());
            assertEquals(1, failover.getRetryableExceptions().size());
        }
    }
    
    @Nested
    @DisplayName("LoadBalancerConfig 测试")
    class LoadBalancerConfigTests {
        
        @Test
        @DisplayName("测试负载均衡配置默认值")
        void testLoadBalancerDefaultValues() {
            MayflyProperties.LoadBalancerConfig lb = new MayflyProperties.LoadBalancerConfig();
            
            assertEquals("round-robin", lb.getStrategy());
            assertNotNull(lb.getHealthCheck());
            assertTrue(lb.getHealthCheck().isEnabled());
            assertEquals(Duration.ofSeconds(30), lb.getHealthCheck().getInterval());
            assertEquals(Duration.ofSeconds(5), lb.getHealthCheck().getTimeout());
            assertEquals(3, lb.getHealthCheck().getUnhealthyThreshold());
        }
        
        @Test
        @DisplayName("测试自定义负载均衡策略")
        void testCustomLoadBalancerStrategy() {
            MayflyProperties.LoadBalancerConfig lb = new MayflyProperties.LoadBalancerConfig();
            lb.setStrategy("weighted-round-robin");
            
            assertEquals("weighted-round-robin", lb.getStrategy());
        }
        
        @Test
        @DisplayName("测试自定义健康检查配置")
        void testCustomHealthCheckConfig() {
            MayflyProperties.HealthCheckConfig healthCheck = new MayflyProperties.HealthCheckConfig();
            
            healthCheck.setEnabled(false);
            healthCheck.setInterval(Duration.ofMinutes(1));
            healthCheck.setTimeout(Duration.ofSeconds(10));
            healthCheck.setUnhealthyThreshold(5);
            
            assertFalse(healthCheck.isEnabled());
            assertEquals(Duration.ofMinutes(1), healthCheck.getInterval());
            assertEquals(Duration.ofSeconds(10), healthCheck.getTimeout());
            assertEquals(5, healthCheck.getUnhealthyThreshold());
        }
    }
    
    @Nested
    @DisplayName("CircuitBreakerConfig 测试")
    class CircuitBreakerConfigTests {
        
        @Test
        @DisplayName("测试熔断器配置默认值")
        void testCircuitBreakerDefaultValues() {
            MayflyProperties.CircuitBreakerConfig cb = new MayflyProperties.CircuitBreakerConfig();
            
            assertTrue(cb.isEnabled());
            assertEquals(50, cb.getFailureRateThreshold());
            assertEquals(Duration.ofSeconds(60), cb.getWaitDurationInOpenState());
            assertEquals(10, cb.getSlidingWindowSize());
            assertEquals(5, cb.getMinimumNumberOfCalls());
        }
        
        @Test
        @DisplayName("测试自定义熔断器配置")
        void testCustomCircuitBreakerConfig() {
            MayflyProperties.CircuitBreakerConfig cb = new MayflyProperties.CircuitBreakerConfig();
            
            cb.setEnabled(false);
            cb.setFailureRateThreshold(30);
            cb.setWaitDurationInOpenState(Duration.ofMinutes(2));
            cb.setSlidingWindowSize(20);
            cb.setMinimumNumberOfCalls(10);
            
            assertFalse(cb.isEnabled());
            assertEquals(30, cb.getFailureRateThreshold());
            assertEquals(Duration.ofMinutes(2), cb.getWaitDurationInOpenState());
            assertEquals(20, cb.getSlidingWindowSize());
            assertEquals(10, cb.getMinimumNumberOfCalls());
        }
    }
    
    @Nested
    @DisplayName("RateLimiterConfig 测试")
    class RateLimiterConfigTests {
        
        @Test
        @DisplayName("测试限流器配置默认值")
        void testRateLimiterDefaultValues() {
            MayflyProperties.RateLimiterConfig rl = new MayflyProperties.RateLimiterConfig();
            
            assertTrue(rl.isEnabled());
            assertEquals(Duration.ofSeconds(1), rl.getLimitRefreshPeriod());
            assertEquals(100, rl.getLimitForPeriod());
            assertEquals(Duration.ZERO, rl.getTimeoutDuration());
        }
        
        @Test
        @DisplayName("测试自定义限流器配置")
        void testCustomRateLimiterConfig() {
            MayflyProperties.RateLimiterConfig rl = new MayflyProperties.RateLimiterConfig();
            
            rl.setEnabled(false);
            rl.setLimitRefreshPeriod(Duration.ofSeconds(5));
            rl.setLimitForPeriod(50);
            rl.setTimeoutDuration(Duration.ofMillis(500));
            
            assertFalse(rl.isEnabled());
            assertEquals(Duration.ofSeconds(5), rl.getLimitRefreshPeriod());
            assertEquals(50, rl.getLimitForPeriod());
            assertEquals(Duration.ofMillis(500), rl.getTimeoutDuration());
        }
    }
    
    @Nested
    @DisplayName("MonitorConfig 测试")
    class MonitorConfigTests {
        
        @Test
        @DisplayName("测试监控配置默认值")
        void testMonitorDefaultValues() {
            MayflyProperties.MonitorConfig monitor = new MayflyProperties.MonitorConfig();
            
            assertTrue(monitor.isEnabled());
        }
        
        @Test
        @DisplayName("测试禁用监控")
        void testDisableMonitor() {
            MayflyProperties.MonitorConfig monitor = new MayflyProperties.MonitorConfig();
            monitor.setEnabled(false);
            
            assertFalse(monitor.isEnabled());
        }
    }
    
    @Nested
    @DisplayName("完整配置测试")
    class FullConfigTests {
        
        @Test
        @DisplayName("测试配置模型列表")
        void testModelListConfiguration() {
            ModelConfig model1 = ModelConfig.builder()
                .name("zhipu-primary")
                .provider("zhipu")
                .model("glm-4")
                .apiKey("${ZHIPU_API_KEY}")
                .weight(70)
                .build();
            
            ModelConfig model2 = ModelConfig.builder()
                .name("tongyi-backup")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey("${TONGYI_API_KEY}")
                .weight(30)
                .build();
            
            properties.getModels().add(model1);
            properties.getModels().add(model2);
            
            assertEquals(2, properties.getModels().size());
            assertEquals("zhipu-primary", properties.getModels().get(0).getName());
            assertEquals("tongyi-backup", properties.getModels().get(1).getName());
        }
        
        @Test
        @DisplayName("测试禁用Mayfly")
        void testDisableMayfly() {
            properties.setEnabled(false);
            
            assertFalse(properties.isEnabled());
        }
        
        @Test
        @DisplayName("测试完整配置组合")
        void testFullConfiguration() {
            properties.setEnabled(true);
            
            ModelConfig model = ModelConfig.builder()
                .name("test-model")
                .provider("test")
                .model("test-v1")
                .apiKey("test-key")
                .build();
            properties.getModels().add(model);
            
            properties.getRouter().setStrategy("weighted");
            properties.getFailover().setMaxRetries(3);
            properties.getLoadbalancer().setStrategy("weighted-round-robin");
            properties.getCircuitBreaker().setFailureRateThreshold(40);
            properties.getRateLimiter().setLimitForPeriod(200);
            properties.getMonitor().setEnabled(true);
            
            assertTrue(properties.isEnabled());
            assertEquals(1, properties.getModels().size());
            assertEquals("weighted", properties.getRouter().getStrategy());
            assertEquals(3, properties.getFailover().getMaxRetries());
            assertEquals("weighted-round-robin", properties.getLoadbalancer().getStrategy());
            assertEquals(40, properties.getCircuitBreaker().getFailureRateThreshold());
            assertEquals(200, properties.getRateLimiter().getLimitForPeriod());
            assertTrue(properties.getMonitor().isEnabled());
        }
    }
}
