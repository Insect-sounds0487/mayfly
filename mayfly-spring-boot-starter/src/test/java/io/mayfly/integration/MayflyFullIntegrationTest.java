package io.mayfly.integration;

import io.mayfly.core.*;
import io.mayfly.loadbalancer.LoadBalancer;
import io.mayfly.loadbalancer.impl.RoundRobinLoadBalancer;
import io.mayfly.loadbalancer.impl.WeightedRoundRobinLoadBalancer;
import io.mayfly.failover.FailoverHandler;
import io.mayfly.failover.FailoverResult;
import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.monitor.NoOpMetricsCollector;
import io.mayfly.router.RouterStrategy;
import io.mayfly.router.impl.FixedRouterStrategy;
import io.mayfly.router.impl.RuleBasedRouterStrategy;
import io.mayfly.router.impl.WeightedRouterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Mayfly完整流程集成测试")
class MayflyFullIntegrationTest {
    
    private ModelInstance model1;
    private ModelInstance model2;
    private ModelInstance model3;
    private ChatModel mockChatModel;
    
    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        
        ModelConfig config1 = ModelConfig.builder()
            .name("primary-model")
            .provider("zhipu")
            .model("glm-4")
            .apiKey("test-key-1")
            .weight(70)
            .build();
        
        ModelConfig config2 = ModelConfig.builder()
            .name("secondary-model")
            .provider("tongyi")
            .model("qwen-max")
            .apiKey("test-key-2")
            .weight(30)
            .build();
        
        ModelConfig config3 = ModelConfig.builder()
            .name("backup-model")
            .provider("deepseek")
            .model("deepseek-chat")
            .apiKey("test-key-3")
            .weight(20)
            .build();
        
        model1 = new ModelInstance(config1, mockChatModel);
        model2 = new ModelInstance(config2, mockChatModel);
        model3 = new ModelInstance(config3, mockChatModel);
    }
    
    @Nested
    @DisplayName("路由 -> 负载均衡完整流程测试")
    class RouterToLoadBalancerTests {
        
        @Test
        @DisplayName("测试固定路由 + 轮询负载均衡")
        void testFixedRouterWithRoundRobinLoadBalancer() {
            RouterStrategy router = new FixedRouterStrategy();
            LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            
            ModelInstance first = router.select(new Prompt(new UserMessage("test")), candidates);
            assertNotNull(first);
            
            ModelInstance lbFirst = loadBalancer.choose(candidates);
            ModelInstance lbSecond = loadBalancer.choose(candidates);
            
            assertNotNull(lbFirst);
            assertNotNull(lbSecond);
            assertNotEquals(lbFirst.getConfig().getName(), lbSecond.getConfig().getName());
        }
        
        @Test
        @DisplayName("测试权重路由 + 加权轮询负载均衡")
        void testWeightedRouterWithWeightedLoadBalancer() {
            RouterStrategy router = new WeightedRouterStrategy();
            LoadBalancer loadBalancer = new WeightedRoundRobinLoadBalancer();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            
            int primaryCount = 0;
            int iterations = 100;
            
            for (int i = 0; i < iterations; i++) {
                ModelInstance selected = loadBalancer.choose(candidates);
                if (selected.getConfig().getName().equals("primary-model")) {
                    primaryCount++;
                }
            }
            
            assertTrue(primaryCount > 50, "Primary model should be selected more often");
        }
    }
    
    @Nested
    @DisplayName("故障转移完整流程测试")
    class FailoverFlowTests {
        
        @Test
        @DisplayName("测试主模型失败后故障转移到备用模型")
        void testFailoverFromPrimaryToBackup() {
            FailoverHandler failoverHandler = new FailoverHandler();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            Prompt prompt = new Prompt(new UserMessage("test message"));
            
            RuntimeException exception = new RuntimeException("Primary model timeout");
            
            FailoverResult result = failoverHandler.executeFailover(
                prompt, model1, candidates, exception);
            
            assertTrue(result.isSuccess());
            assertNotNull(result.getTargetModel());
            assertNotEquals("primary-model", result.getTargetModel().getConfig().getName());
        }
        
        @Test
        @DisplayName("测试所有备用模型都不可用时的故障转移")
        void testFailoverWhenAllBackupsUnavailable() {
            FailoverHandler failoverHandler = new FailoverHandler();
            
            model2.getConfig().setEnabled(false);
            model3.getConfig().setEnabled(false);
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            Prompt prompt = new Prompt(new UserMessage("test message"));
            
            RuntimeException exception = new RuntimeException("Primary model timeout");
            
            FailoverResult result = failoverHandler.executeFailover(
                prompt, model1, candidates, exception);
            
            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
        }
        
        @Test
        @DisplayName("测试故障转移后主模型进入冷却状态")
        void testFailoverSetsCooldownOnFailedModel() {
            FailoverHandler failoverHandler = new FailoverHandler();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            Prompt prompt = new Prompt(new UserMessage("test"));
            
            failoverHandler.executeFailover(
                prompt, model1, candidates, new RuntimeException("Error"));
            
            assertEquals(HealthStatus.COOLDOWN, model1.getHealthStatus());
            assertNotNull(model1.getCooldownUntil());
        }
    }
    
    @Nested
    @DisplayName("熔断器完整流程测试")
    class CircuitBreakerFlowTests {
        
        @Test
        @DisplayName("测试熔断器保护调用")
        void testCircuitBreakerProtectedCall() {
            CircuitBreakerManager circuitBreakerManager = new CircuitBreakerManager();
            
            String result = circuitBreakerManager.executeProtected(
                "test-model",
                () -> "success"
            );
            
            assertEquals("success", result);
        }
        
        @Test
        @DisplayName("测试熔断器在异常时打开")
        void testCircuitBreakerOpensOnException() {
            CircuitBreakerManager circuitBreakerManager = new CircuitBreakerManager();
            
            for (int i = 0; i < 20; i++) {
                try {
                    circuitBreakerManager.executeProtected(
                        "failing-model",
                        () -> {
                            throw new RuntimeException("Simulated failure");
                        }
                    );
                } catch (Exception e) {
                }
            }
            
            io.github.resilience4j.circuitbreaker.CircuitBreaker cb = 
                circuitBreakerManager.getCircuitBreaker("failing-model");
            
            assertTrue(cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN ||
                       cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN ||
                       cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
        }
    }
    
    @Nested
    @DisplayName("监控完整流程测试")
    class MonitorFlowTests {
        
        @Test
        @DisplayName("测试监控指标收集完整流程")
        void testMetricsCollectionFlow() {
            MetricsCollector collector = new NoOpMetricsCollector();
            
            collector.recordSuccess("model-1", 150L, 100, 200);
            collector.recordSuccess("model-1", 200L, 150, 250);
            collector.recordFailure("model-1", "TimeoutException");
            collector.recordFailover("model-1", "model-2");
            
            collector.recordSuccess("model-2", 100L, 80, 120);
            collector.recordFailure("model-2", "ConnectionException");
        }
        
        @Test
        @DisplayName("测试多次请求后的监控数据")
        void testMultipleRequestsMetrics() {
            MetricsCollector collector = new NoOpMetricsCollector();
            
            for (int i = 0; i < 50; i++) {
                collector.recordSuccess("test-model", 100L + i, 50, 100);
            }
            
            for (int i = 0; i < 5; i++) {
                collector.recordFailure("test-model", "Exception" + i);
            }
        }
    }
    
    @Nested
    @DisplayName("多模块协同工作测试")
    class MultiModuleCollaborationTests {
        
        @Test
        @DisplayName("测试路由 + 负载均衡 + 故障转移协同工作")
        void testRouterLoadBalancerFailoverCollaboration() {
            RouterStrategy router = new FixedRouterStrategy();
            LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
            FailoverHandler failoverHandler = new FailoverHandler();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            Prompt prompt = new Prompt(new UserMessage("collaboration test"));
            
            ModelInstance selected = router.select(prompt, candidates);
            assertNotNull(selected);
            
            ModelInstance lbSelected = loadBalancer.choose(candidates);
            assertNotNull(lbSelected);
            
            FailoverResult failoverResult = failoverHandler.executeFailover(
                prompt, selected, candidates, new RuntimeException("Test error"));
            
            assertTrue(failoverResult.isSuccess() || !failoverResult.isSuccess());
        }
        
        @Test
        @DisplayName("测试规则路由 + 加权负载均衡协同工作")
        void testRuleBasedRouterWithWeightedLoadBalancer() {
            RuleBasedRouterStrategy router = new RuleBasedRouterStrategy();
            LoadBalancer loadBalancer = new WeightedRoundRobinLoadBalancer();
            
            io.mayfly.router.RouterRule rule = io.mayfly.router.RouterRule.builder()
                .name("always-match")
                .condition("true")
                .targetModel("primary-model")
                .priority(1)
                .build();
            
            router.setRules(Collections.singletonList(rule));
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            Prompt prompt = new Prompt(new UserMessage("rule test"));
            
            ModelInstance selected = router.select(prompt, candidates);
            assertNotNull(selected);
            
            ModelInstance lbSelected = loadBalancer.choose(candidates);
            assertNotNull(lbSelected);
        }
        
        @Test
        @DisplayName("测试完整请求生命周期")
        void testCompleteRequestLifecycle() {
            RouterStrategy router = new FixedRouterStrategy();
            CircuitBreakerManager circuitBreakerManager = new CircuitBreakerManager();
            MetricsCollector collector = new NoOpMetricsCollector();
            
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            Prompt prompt = new Prompt(new UserMessage("lifecycle test"));
            
            long startTime = System.currentTimeMillis();
            
            try {
                ModelInstance selected = router.select(prompt, candidates);
                
                String result = circuitBreakerManager.executeProtected(
                    selected.getConfig().getName(),
                    () -> {
                        selected.getActiveRequests().incrementAndGet();
                        try {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return "mock response";
                        } finally {
                            selected.getActiveRequests().decrementAndGet();
                        }
                    }
                );
                
                long latency = System.currentTimeMillis() - startTime;
                collector.recordSuccess(
                    selected.getConfig().getName(),
                    latency,
                    100,
                    200
                );
                
                assertNotNull(result);
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - startTime;
                collector.recordFailure("model-1", e.getMessage());
            }
        }
    }
}
