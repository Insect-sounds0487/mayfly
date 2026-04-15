package io.mayfly.loadbalancer.impl;

import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * RoundRobinLoadBalancer 单元测试
 */
class RoundRobinLoadBalancerTest {
    
    private RoundRobinLoadBalancer loadBalancer;
    private List<ModelInstance> candidates;
    
    @BeforeEach
    void setUp() {
        loadBalancer = new RoundRobinLoadBalancer();
        
        ModelConfig config1 = ModelConfig.builder()
            .name("model-1")
            .provider("test")
            .model("test-v1")
            .apiKey("key1")
            .weight(100)
            .build();
        
        ModelConfig config2 = ModelConfig.builder()
            .name("model-2")
            .provider("test")
            .model("test-v1")
            .apiKey("key2")
            .weight(50)
            .build();
        
        ChatModel mockModel1 = mock(ChatModel.class);
        ChatModel mockModel2 = mock(ChatModel.class);
        
        candidates = Arrays.asList(
            new ModelInstance(config1, mockModel1),
            new ModelInstance(config2, mockModel2)
        );
    }
    
    @Test
    @DisplayName("测试轮询 - 均匀分配")
    void testRoundRobinDistribution() {
        ModelInstance first = loadBalancer.choose(candidates);
        ModelInstance second = loadBalancer.choose(candidates);
        
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.getConfig().getName(), second.getConfig().getName());
    }
    
    @Test
    @DisplayName("测试轮询 - 跳过不可用模型")
    void testRoundRobinSkipsUnavailable() {
        candidates.get(0).setHealthStatus(io.mayfly.core.HealthStatus.UNHEALTHY);
        
        ModelInstance first = loadBalancer.choose(candidates);
        ModelInstance second = loadBalancer.choose(candidates);
        
        assertEquals("model-2", first.getConfig().getName());
        assertEquals("model-2", second.getConfig().getName());
    }
    
    @Test
    @DisplayName("测试轮询 - 所有模型不可用时抛出异常")
    void testRoundRobinThrowsWhenAllUnavailable() {
        candidates.get(0).setHealthStatus(io.mayfly.core.HealthStatus.UNHEALTHY);
        candidates.get(1).setHealthStatus(io.mayfly.core.HealthStatus.UNHEALTHY);
        
        assertThrows(ModelUnavailableException.class, () -> {
            loadBalancer.choose(candidates);
        });
    }
    
    @Test
    @DisplayName("测试负载均衡器名称")
    void testLoadBalancerName() {
        assertEquals("round-robin", loadBalancer.getName());
    }
}
