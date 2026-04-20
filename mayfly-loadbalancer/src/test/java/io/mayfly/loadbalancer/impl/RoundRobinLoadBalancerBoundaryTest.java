package io.mayfly.loadbalancer.impl;

import io.mayfly.core.HealthStatus;
import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("RoundRobinLoadBalancer 边界条件测试")
class RoundRobinLoadBalancerBoundaryTest {
    
    private RoundRobinLoadBalancer loadBalancer;
    private ChatModel mockChatModel;
    
    @BeforeEach
    void setUp() {
        loadBalancer = new RoundRobinLoadBalancer();
        mockChatModel = mock(ChatModel.class);
    }
    
    @Nested
    @DisplayName("空列表和单元素测试")
    class EmptyAndSingleTests {
        
        @Test
        @DisplayName("测试空列表抛出异常")
        void testEmptyListThrowsException() {
            assertThrows(ModelUnavailableException.class, () -> {
                loadBalancer.choose(Collections.emptyList());
            });
        }
        
        @Test
        @DisplayName("测试单个可用模型")
        void testSingleAvailableModel() {
            ModelConfig config = ModelConfig.builder()
                .name("single-model")
                .provider("test")
                .model("test-v1")
                .apiKey("key")
                .build();
            
            ModelInstance instance = new ModelInstance(config, mockChatModel);
            List<ModelInstance> candidates = Collections.singletonList(instance);
            
            ModelInstance selected = loadBalancer.choose(candidates);
            assertEquals("single-model", selected.getConfig().getName());
        }
        
        @Test
        @DisplayName("测试单个不可用模型抛出异常")
        void testSingleUnavailableModelThrowsException() {
            ModelConfig config = ModelConfig.builder()
                .name("unavailable-model")
                .provider("test")
                .model("test-v1")
                .apiKey("key")
                .enabled(false)
                .build();
            
            ModelInstance instance = new ModelInstance(config, mockChatModel);
            List<ModelInstance> candidates = Collections.singletonList(instance);
            
            assertThrows(ModelUnavailableException.class, () -> {
                loadBalancer.choose(candidates);
            });
        }
    }
    
    @Nested
    @DisplayName("多模型轮询测试")
    class MultiModelRoundRobinTests {
        
        @Test
        @DisplayName("测试三个模型的轮询顺序")
        void testThreeModelRoundRobinOrder() {
            ModelConfig config1 = ModelConfig.builder()
                .name("model-a")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .build();
            
            ModelConfig config2 = ModelConfig.builder()
                .name("model-b")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .build();
            
            ModelConfig config3 = ModelConfig.builder()
                .name("model-c")
                .provider("test")
                .model("test-v1")
                .apiKey("key3")
                .build();
            
            List<ModelInstance> candidates = Arrays.asList(
                new ModelInstance(config1, mockChatModel),
                new ModelInstance(config2, mockChatModel),
                new ModelInstance(config3, mockChatModel)
            );
            
            ModelInstance first = loadBalancer.choose(candidates);
            ModelInstance second = loadBalancer.choose(candidates);
            ModelInstance third = loadBalancer.choose(candidates);
            ModelInstance fourth = loadBalancer.choose(candidates);
            
            assertEquals("model-a", first.getConfig().getName());
            assertEquals("model-b", second.getConfig().getName());
            assertEquals("model-c", third.getConfig().getName());
            assertEquals("model-a", fourth.getConfig().getName());
        }
        
        @Test
        @DisplayName("测试大量轮询的均匀分布")
        void testLargeNumberOfRounds() {
            ModelConfig config1 = ModelConfig.builder()
                .name("model-x")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .build();
            
            ModelConfig config2 = ModelConfig.builder()
                .name("model-y")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .build();
            
            List<ModelInstance> candidates = Arrays.asList(
                new ModelInstance(config1, mockChatModel),
                new ModelInstance(config2, mockChatModel)
            );
            
            int modelXCount = 0;
            int modelYCount = 0;
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                ModelInstance selected = loadBalancer.choose(candidates);
                if (selected.getConfig().getName().equals("model-x")) {
                    modelXCount++;
                } else {
                    modelYCount++;
                }
            }
            
            assertEquals(500, modelXCount);
            assertEquals(500, modelYCount);
        }
    }
    
    @Nested
    @DisplayName("健康状态边界测试")
    class HealthStatusBoundaryTests {
        
        @Test
        @DisplayName("测试冷却状态但冷却已结束的模型可用")
        void testCooldownExpiredModelAvailable() {
            ModelConfig config1 = ModelConfig.builder()
                .name("cooldown-model")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .build();
            
            ModelInstance cooldownModel = new ModelInstance(config1, mockChatModel);
            cooldownModel.setHealthStatus(HealthStatus.COOLDOWN);
            cooldownModel.setCooldownUntil(Instant.now().minusSeconds(10));
            
            ModelConfig config2 = ModelConfig.builder()
                .name("healthy-model")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .build();
            
            ModelInstance healthyModel = new ModelInstance(config2, mockChatModel);
            
            List<ModelInstance> candidates = Arrays.asList(cooldownModel, healthyModel);
            
            int cooldownCount = 0;
            int healthyCount = 0;
            
            for (int i = 0; i < 10; i++) {
                ModelInstance selected = loadBalancer.choose(candidates);
                if (selected.getConfig().getName().equals("cooldown-model")) {
                    cooldownCount++;
                } else {
                    healthyCount++;
                }
            }
            
            assertTrue(cooldownCount > 0, "Cooldown expired model should be selected");
            assertTrue(healthyCount > 0, "Healthy model should be selected");
        }
        
        @Test
        @DisplayName("测试部分模型处于UNHEALTHY状态")
        void testSomeModelsUnhealthy() {
            ModelConfig config1 = ModelConfig.builder()
                .name("unhealthy-1")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .build();
            
            ModelConfig config2 = ModelConfig.builder()
                .name("healthy-1")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .build();
            
            ModelConfig config3 = ModelConfig.builder()
                .name("unhealthy-2")
                .provider("test")
                .model("test-v1")
                .apiKey("key3")
                .build();
            
            ModelInstance unhealthy1 = new ModelInstance(config1, mockChatModel);
            unhealthy1.setHealthStatus(HealthStatus.UNHEALTHY);
            
            ModelInstance healthy1 = new ModelInstance(config2, mockChatModel);
            
            ModelInstance unhealthy2 = new ModelInstance(config3, mockChatModel);
            unhealthy2.setHealthStatus(HealthStatus.UNHEALTHY);
            
            List<ModelInstance> candidates = Arrays.asList(unhealthy1, healthy1, unhealthy2);
            
            for (int i = 0; i < 10; i++) {
                ModelInstance selected = loadBalancer.choose(candidates);
                assertEquals("healthy-1", selected.getConfig().getName());
            }
        }
    }
    
    @Nested
    @DisplayName("权重边界测试")
    class WeightBoundaryTests {
        
        @Test
        @DisplayName("测试权重为0的模型")
        void testModelWithZeroWeight() {
            ModelConfig config1 = ModelConfig.builder()
                .name("zero-weight")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .weight(0)
                .build();
            
            ModelConfig config2 = ModelConfig.builder()
                .name("normal-weight")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .weight(100)
                .build();
            
            List<ModelInstance> candidates = Arrays.asList(
                new ModelInstance(config1, mockChatModel),
                new ModelInstance(config2, mockChatModel)
            );
            
            ModelInstance first = loadBalancer.choose(candidates);
            ModelInstance second = loadBalancer.choose(candidates);
            
            assertNotNull(first);
            assertNotNull(second);
        }
        
        @Test
        @DisplayName("测试权重差异很大的模型")
        void testModelsWithLargeWeightDifference() {
            ModelConfig config1 = ModelConfig.builder()
                .name("low-weight")
                .provider("test")
                .model("test-v1")
                .apiKey("key1")
                .weight(1)
                .build();
            
            ModelConfig config2 = ModelConfig.builder()
                .name("high-weight")
                .provider("test")
                .model("test-v1")
                .apiKey("key2")
                .weight(1000)
                .build();
            
            List<ModelInstance> candidates = Arrays.asList(
                new ModelInstance(config1, mockChatModel),
                new ModelInstance(config2, mockChatModel)
            );
            
            ModelInstance selected = loadBalancer.choose(candidates);
            assertNotNull(selected);
        }
    }
}
