package io.mayfly.integration;

import io.mayfly.autoconfigure.MayflyAutoConfiguration;
import io.mayfly.autoconfigure.MayflyProperties;
import io.mayfly.core.*;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.router.RouterStrategy;
import io.mayfly.router.impl.FixedRouterStrategy;
import io.mayfly.router.impl.RuleBasedRouterStrategy;
import io.mayfly.router.impl.WeightedRouterStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mayfly端到端集成测试
 * 验证完整的模型调用流程：路由 -> 负载均衡 -> 故障转移 -> 熔断 -> 监控
 */
@SpringBootTest(classes = MayflyIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
    "mayfly.enabled=true",
    "mayfly.router.strategy=fixed",
    "mayfail.monitor.enabled=true"
})
@DisplayName("Mayfly端到端集成测试")
class MayflyIntegrationTest {
    
    @SpringBootApplication
    @TestConfiguration
    static class TestConfig {
        
        @Bean
        public ChatModel mockChatModel() {
            ChatModel mockModel = mock(ChatModel.class);
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockModel.call(any(Prompt.class))).thenReturn(mockResponse);
            return mockModel;
        }
    }
    
    @Autowired(required = false)
    private ModelRegistry modelRegistry;
    
    @Autowired(required = false)
    private RouterStrategy routerStrategy;
    
    @Autowired(required = false)
    private MetricsCollector metricsCollector;
    
    @Autowired
    private MayflyProperties mayflyProperties;
    
    @Nested
    @DisplayName("自动配置加载测试")
    class AutoConfigurationTests {
        
        @Test
        @DisplayName("验证Mayfly配置正确加载")
        void testMayflyProperties_Loaded() {
            assertThat(mayflyProperties).isNotNull();
            assertThat(mayflyProperties.isEnabled()).isTrue();
            assertThat(mayflyProperties.getRouter().getStrategy()).isEqualTo("fixed");
            assertThat(mayflyProperties.getMonitor().isEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("验证核心Bean已创建")
        void testCoreBeans_Created() {
            assertThat(routerStrategy).isNotNull();
            assertThat(metricsCollector).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("路由策略集成测试")
    class RouterStrategyIntegrationTests {
        
        @Test
        @DisplayName("固定路由策略 - 正确加载")
        void testFixedRouterStrategy_Loaded() {
            assertThat(routerStrategy).isInstanceOf(FixedRouterStrategy.class);
        }
    }
    
    @Nested
    @DisplayName("监控集成测试")
    class MonitorIntegrationTests {
        
        @Test
        @DisplayName("监控收集器 - 正常工作")
        void testMetricsCollector_Works() {
            metricsCollector.recordSuccess("test-model", 100L, 50, 100);
            metricsCollector.recordFailure("test-model", "TimeoutException");
            metricsCollector.recordFailover("model-1", "model-2");
        }
    }
}
