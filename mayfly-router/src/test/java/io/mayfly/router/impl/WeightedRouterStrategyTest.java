package io.mayfly.router.impl;

import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * WeightedRouterStrategy 单元测试
 */
class WeightedRouterStrategyTest {
    
    private WeightedRouterStrategy strategy;
    private ModelInstance highWeightModel;
    private ModelInstance lowWeightModel;
    private List<ModelInstance> candidates;
    private Prompt request;
    
    @BeforeEach
    void setUp() {
        strategy = new WeightedRouterStrategy();
        ChatModel mockChatModel = mock(ChatModel.class);
        
        ModelConfig highConfig = ModelConfig.builder()
            .name("high-weight-model")
            .provider("zhipu")
            .model("glm-4")
            .weight(80)
            .build();
        highWeightModel = new ModelInstance(highConfig, mockChatModel);
        
        ModelConfig lowConfig = ModelConfig.builder()
            .name("low-weight-model")
            .provider("tongyi")
            .model("qwen-max")
            .weight(20)
            .build();
        lowWeightModel = new ModelInstance(lowConfig, mockChatModel);
        
        candidates = Arrays.asList(highWeightModel, lowWeightModel);
        request = new Prompt(new UserMessage("Test message"));
    }
    
    @Test
    void testWeightedSelection_Distribution() {
        int highCount = 0;
        int lowCount = 0;
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            ModelInstance selected = strategy.select(request, candidates);
            if (selected.getConfig().getName().equals("high-weight-model")) {
                highCount++;
            } else {
                lowCount++;
            }
        }
        
        double highRatio = (double) highCount / iterations;
        assertTrue(highRatio > 0.7 && highRatio < 0.9, 
            "High weight model should be selected ~80% of the time, actual: " + highRatio);
    }
    
    @Test
    void testWeightedSelection_SkipUnavailable() {
        highWeightModel.getConfig().setEnabled(false);
        
        ModelInstance selected = strategy.select(request, candidates);
        assertEquals("low-weight-model", selected.getConfig().getName());
    }
    
    @Test
    void testWeightedSelection_AllUnavailable_ThrowsException() {
        highWeightModel.getConfig().setEnabled(false);
        lowWeightModel.getConfig().setEnabled(false);
        
        assertThrows(ModelUnavailableException.class, () -> {
            strategy.select(request, candidates);
        });
    }
    
    @Test
    void testWeightedSelection_EmptyList_ThrowsException() {
        assertThrows(ModelUnavailableException.class, () -> {
            strategy.select(request, Collections.emptyList());
        });
    }
    
    @Test
    void testWeightedSelection_SingleModel() {
        List<ModelInstance> singleCandidate = Collections.singletonList(highWeightModel);
        
        ModelInstance selected = strategy.select(request, singleCandidate);
        assertEquals("high-weight-model", selected.getConfig().getName());
    }
    
    @Test
    void testGetName() {
        assertEquals("weighted", strategy.getName());
    }
    
    @Test
    void testWeightedSelection_ThreeModels() {
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
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            ModelInstance selected = strategy.select(request, threeCandidates);
            String name = selected.getConfig().getName();
            if (name.equals("high-weight-model")) highCount++;
            else if (name.equals("medium-weight-model")) mediumCount++;
            else lowCount++;
        }
        
        double highRatio = (double) highCount / iterations;
        double mediumRatio = (double) mediumCount / iterations;
        double lowRatio = (double) lowCount / iterations;
        
        assertTrue(highRatio > mediumRatio && mediumRatio > lowRatio,
            "Selection frequency should match weight order");
    }
}
