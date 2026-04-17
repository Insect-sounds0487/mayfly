package io.mayfly.loadbalancer.impl;

import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * WeightedRoundRobinLoadBalancer 单元测试
 */
class WeightedRoundRobinLoadBalancerTest {
    
    private WeightedRoundRobinLoadBalancer loadBalancer;
    private ModelInstance highWeightModel;
    private ModelInstance lowWeightModel;
    private List<ModelInstance> candidates;
    
    @BeforeEach
    void setUp() {
        loadBalancer = new WeightedRoundRobinLoadBalancer();
        ChatModel mockChatModel = mock(ChatModel.class);
        
        ModelConfig highConfig = ModelConfig.builder()
            .name("high-weight-model")
            .provider("zhipu")
            .model("glm-4")
            .weight(70)
            .build();
        highWeightModel = new ModelInstance(highConfig, mockChatModel);
        
        ModelConfig lowConfig = ModelConfig.builder()
            .name("low-weight-model")
            .provider("tongyi")
            .model("qwen-max")
            .weight(30)
            .build();
        lowWeightModel = new ModelInstance(lowConfig, mockChatModel);
        
        candidates = Arrays.asList(highWeightModel, lowWeightModel);
    }
    
    @Test
    void testWeightedRoundRobin_Distribution() {
        int highCount = 0;
        int lowCount = 0;
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            ModelInstance selected = loadBalancer.choose(candidates);
            if (selected.getConfig().getName().equals("high-weight-model")) {
                highCount++;
            } else {
                lowCount++;
            }
        }
        
        assertTrue(highCount > lowCount, 
            "High weight model should be selected more often");
        
        double ratio = (double) highCount / iterations;
        assertTrue(ratio > 0.6 && ratio < 0.8, 
            "High weight model ratio should be around 70%, actual: " + ratio);
    }
    
    @Test
    void testWeightedRoundRobin_SkipUnavailable() {
        highWeightModel.getConfig().setEnabled(false);
        
        for (int i = 0; i < 10; i++) {
            ModelInstance selected = loadBalancer.choose(candidates);
            assertEquals("low-weight-model", selected.getConfig().getName());
        }
    }
    
    @Test
    void testWeightedRoundRobin_AllUnavailable_ThrowsException() {
        highWeightModel.getConfig().setEnabled(false);
        lowWeightModel.getConfig().setEnabled(false);
        
        assertThrows(ModelUnavailableException.class, () -> {
            loadBalancer.choose(candidates);
        });
    }
    
    @Test
    void testWeightedRoundRobin_EmptyList_ThrowsException() {
        assertThrows(ModelUnavailableException.class, () -> {
            loadBalancer.choose(Collections.emptyList());
        });
    }
    
    @Test
    void testWeightedRoundRobin_SingleModel() {
        List<ModelInstance> singleCandidate = Collections.singletonList(highWeightModel);
        
        for (int i = 0; i < 5; i++) {
            ModelInstance selected = loadBalancer.choose(singleCandidate);
            assertEquals("high-weight-model", selected.getConfig().getName());
        }
    }
    
    @Test
    void testGetName() {
        assertEquals("weighted-round-robin", loadBalancer.getName());
    }
    
    @Test
    void testWeightedRoundRobin_ThreeModels() {
        ChatModel mockChatModel = mock(ChatModel.class);
        ModelConfig mediumConfig = ModelConfig.builder()
            .name("medium-weight-model")
            .provider("deepseek")
            .model("deepseek-coder")
            .weight(50)
            .build();
        ModelInstance mediumWeightModel = new ModelInstance(mediumConfig, mockChatModel);
        
        List<ModelInstance> threeCandidates = Arrays.asList(
            highWeightModel, mediumWeightModel, lowWeightModel);
        
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            ModelInstance selected = loadBalancer.choose(threeCandidates);
            String name = selected.getConfig().getName();
            if (name.equals("high-weight-model")) highCount++;
            else if (name.equals("medium-weight-model")) mediumCount++;
            else lowCount++;
        }
        
        assertTrue(highCount > mediumCount && mediumCount > lowCount,
            "Selection count should match weight order: high=" + highCount + 
            ", medium=" + mediumCount + ", low=" + lowCount);
    }
    
    @Test
    void testWeightedRoundRobin_Stateful() {
        ModelInstance selected1 = loadBalancer.choose(candidates);
        ModelInstance selected2 = loadBalancer.choose(candidates);
        
        assertNotNull(selected1);
        assertNotNull(selected2);
    }
    
    @Test
    void testWeightedRoundRobin_DynamicWeightChange() {
        ModelInstance selected = loadBalancer.choose(candidates);
        
        highWeightModel.getConfig().setWeight(90);
        lowWeightModel.getConfig().setWeight(10);
        
        int highCount = 0;
        int iterations = 50;
        
        for (int i = 0; i < iterations; i++) {
            ModelInstance s = loadBalancer.choose(candidates);
            if (s.getConfig().getName().equals("high-weight-model")) {
                highCount++;
            }
        }
        
        double ratio = (double) highCount / iterations;
        assertTrue(ratio > 0.8, 
            "After weight change, high weight model should be selected more often, actual: " + ratio);
    }
}
