package io.mayfly.router.impl;

import io.mayfly.core.HealthStatus;
import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.router.RouterRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleBasedRouterStrategy 单元测试
 */
@DisplayName("规则路由策略测试")
class RuleBasedRouterStrategyTest {
    
    private RuleBasedRouterStrategy strategy;
    private ModelInstance model1;
    private ModelInstance model2;
    private ModelInstance model3;
    
    @BeforeEach
    void setUp() {
        strategy = new RuleBasedRouterStrategy();
        
        ModelConfig config1 = ModelConfig.builder()
            .name("model-1")
            .provider("test")
            .model("test-model-1")
            .build();
        
        ModelConfig config2 = ModelConfig.builder()
            .name("model-2")
            .provider("test")
            .model("test-model-2")
            .build();
        
        ModelConfig config3 = ModelConfig.builder()
            .name("model-3")
            .provider("test")
            .model("test-model-3")
            .build();
        
        model1 = new ModelInstance(config1, null);
        model2 = new ModelInstance(config2, null);
        model3 = new ModelInstance(config3, null);
    }
    
    @Nested
    @DisplayName("基本规则匹配测试")
    class BasicRuleMatchingTests {
        
        @Test
        @DisplayName("单条规则 - 条件为true时匹配")
        void testSingleRule_TrueCondition() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("always-match")
                    .condition("true")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            
            ModelInstance selected = strategy.select(prompt, candidates);
            
            assertEquals("model-1", selected.getConfig().getName());
        }
        
        @Test
        @DisplayName("单条规则 - 条件为false时不匹配")
        void testSingleRule_FalseCondition() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("never-match")
                    .condition("false")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            
            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(prompt, candidates);
            });
        }
    }
    
    @Nested
    @DisplayName("优先级测试")
    class PriorityTests {
        
        @Test
        @DisplayName("多条规则 - 选择优先级最高的匹配规则")
        void testMultipleRules_HighestPriorityMatches() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("low-priority")
                    .condition("true")
                    .targetModel("model-2")
                    .priority(100)
                    .build(),
                RouterRule.builder()
                    .name("high-priority")
                    .condition("true")
                    .targetModel("model-1")
                    .priority(1)
                    .build(),
                RouterRule.builder()
                    .name("medium-priority")
                    .condition("true")
                    .targetModel("model-3")
                    .priority(50)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            
            ModelInstance selected = strategy.select(prompt, candidates);
            
            assertEquals("model-1", selected.getConfig().getName());
        }
        
        @Test
        @DisplayName("高优先级规则不匹配 - 选择次高优先级")
        void testMultipleRules_HighPriorityNotMatch_SelectNext() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("high-priority")
                    .condition("false")
                    .targetModel("model-1")
                    .priority(1)
                    .build(),
                RouterRule.builder()
                    .name("medium-priority")
                    .condition("true")
                    .targetModel("model-2")
                    .priority(50)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2, model3);
            
            ModelInstance selected = strategy.select(prompt, candidates);
            
            assertEquals("model-2", selected.getConfig().getName());
        }
    }
    
    @Nested
    @DisplayName("SpEL表达式测试")
    class SpelExpressionTests {
        
        @Test
        @DisplayName("SpEL表达式 - 基本布尔值")
        void testSpelExpression_BooleanValue() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("true-rule")
                    .condition("true")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            
            ModelInstance selected = strategy.select(prompt, candidates);
            assertEquals("model-1", selected.getConfig().getName());
        }
        
        @Test
        @DisplayName("SpEL表达式解析失败 - 返回false")
        void testSpelExpression_ParseError_ReturnsFalse() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("invalid-rule")
                    .condition("invalid syntax {{{")
                    .targetModel("model-1")
                    .priority(1)
                    .build(),
                RouterRule.builder()
                    .name("fallback-rule")
                    .condition("true")
                    .targetModel("model-2")
                    .priority(2)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            
            ModelInstance selected = strategy.select(prompt, candidates);
            assertEquals("model-2", selected.getConfig().getName());
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("目标模型不可用 - 抛出异常")
        void testTargetModelNotAvailable_ThrowsException() {
            model1.setHealthStatus(HealthStatus.UNHEALTHY);
            
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("rule-1")
                    .condition("true")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            
            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(prompt, candidates);
            });
        }
        
        @Test
        @DisplayName("无匹配规则 - 抛出异常")
        void testNoMatchingRule_ThrowsException() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("rule-1")
                    .condition("false")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            List<ModelInstance> candidates = Arrays.asList(model1, model2);
            
            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(prompt, candidates);
            });
        }
        
        @Test
        @DisplayName("空候选列表 - 抛出异常")
        void testEmptyCandidates_ThrowsException() {
            List<RouterRule> rules = Arrays.asList(
                RouterRule.builder()
                    .name("rule-1")
                    .condition("true")
                    .targetModel("model-1")
                    .priority(1)
                    .build()
            );
            strategy.setRules(rules);
            
            Prompt prompt = new Prompt(new UserMessage("test"));
            
            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(prompt, Collections.emptyList());
            });
        }
    }
    
    @Nested
    @DisplayName("策略名称和顺序测试")
    class StrategyMetadataTests {
        
        @Test
        @DisplayName("获取策略名称")
        void testGetName() {
            assertEquals("rule-based", strategy.getName());
        }
        
        @Test
        @DisplayName("获取策略顺序")
        void testGetOrder() {
            assertEquals(50, strategy.getOrder());
        }
    }
}
