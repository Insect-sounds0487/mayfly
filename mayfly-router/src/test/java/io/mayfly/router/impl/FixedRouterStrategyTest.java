package io.mayfly.router.impl;

import io.mayfly.core.HealthStatus;
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
 * FixedRouterStrategy 单元测试
 */
class FixedRouterStrategyTest {
    
    private FixedRouterStrategy strategy;
    private List<ModelInstance> candidates;
    
    @BeforeEach
    void setUp() {
        strategy = new FixedRouterStrategy();
        
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
    @DisplayName("测试固定路由 - 选择第一个可用模型")
    void testFixedRouterSelectsFirstAvailable() {
        ModelInstance selected = strategy.select(null, candidates);
        
        assertNotNull(selected);
        assertEquals("model-1", selected.getConfig().getName());
    }
    
    @Test
    @DisplayName("测试固定路由 - 跳过不可用模型")
    void testFixedRouterSkipsUnavailable() {
        candidates.get(0).setHealthStatus(HealthStatus.UNHEALTHY);
        
        ModelInstance selected = strategy.select(null, candidates);
        
        assertNotNull(selected);
        assertEquals("model-2", selected.getConfig().getName());
    }
    
    @Test
    @DisplayName("测试固定路由 - 所有模型不可用时抛出异常")
    void testFixedRouterThrowsWhenAllUnavailable() {
        candidates.get(0).setHealthStatus(HealthStatus.UNHEALTHY);
        candidates.get(1).setHealthStatus(HealthStatus.UNHEALTHY);
        
        assertThrows(ModelUnavailableException.class, () -> {
            strategy.select(null, candidates);
        });
    }
    
    @Test
    @DisplayName("测试策略名称")
    void testStrategyName() {
        assertEquals("fixed", strategy.getName());
    }
    
    @Test
    @DisplayName("测试策略优先级")
    void testStrategyOrder() {
        assertEquals(100, strategy.getOrder());
    }
}
