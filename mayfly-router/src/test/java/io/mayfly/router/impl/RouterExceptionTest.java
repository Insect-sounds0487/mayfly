package io.mayfly.router.impl;

import io.mayfly.core.*;
import io.mayfly.router.RouterRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Router 模块异常测试
 */
@DisplayName("Router 模块异常测试")
class RouterExceptionTest {

    private ModelInstance createModelInstance(String name, String provider, int weight, HealthStatus status) {
        ModelConfig config = ModelConfig.builder()
            .name(name)
            .provider(provider)
            .model("test-model")
            .apiKey("test-key")
            .weight(weight)
            .build();
        ChatModel mockModel = mock(ChatModel.class);
        ModelInstance instance = new ModelInstance(config, mockModel);
        instance.setHealthStatus(status);
        return instance;
    }

    @Nested
    @DisplayName("FixedRouterStrategy 异常测试")
    class FixedRouterStrategyExceptionTests {

        private final FixedRouterStrategy strategy = new FixedRouterStrategy();

        @Test
        @DisplayName("空候选列表抛出异常")
        void testEmptyCandidateList() {
            List<ModelInstance> emptyList = new ArrayList<>();

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), emptyList);
            });
        }

        @Test
        @DisplayName("null 候选列表抛出异常")
        void testNullCandidateList() {
            assertThrows(NullPointerException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), null);
            });
        }

        @Test
        @DisplayName("null prompt 正常处理")
        void testNullPrompt() {
            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertDoesNotThrow(() -> strategy.select(null, candidates));
        }

        @Test
        @DisplayName("所有模型处于冷却状态抛出异常")
        void testAllModelsInCooldown() {
            ModelInstance m1 = createModelInstance("model-1", "test", 100, HealthStatus.COOLDOWN);
            m1.setCooldownUntil(java.time.Instant.now().plusSeconds(60));
            ModelInstance m2 = createModelInstance("model-2", "test", 50, HealthStatus.COOLDOWN);
            m2.setCooldownUntil(java.time.Instant.now().plusSeconds(60));

            List<ModelInstance> candidates = List.of(m1, m2);

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }
    }

    @Nested
    @DisplayName("RuleBasedRouterStrategy 异常测试")
    class RuleBasedRouterStrategyExceptionTests {

        private final RuleBasedRouterStrategy strategy = new RuleBasedRouterStrategy();

        @Test
        @DisplayName("无规则时抛出异常")
        void testNoRules() {
            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }

        @Test
        @DisplayName("规则条件表达式语法错误时跳过该规则")
        void testInvalidSpelExpression() {
            RouterRule invalidRule = RouterRule.builder()
                .name("invalid-rule")
                .condition("#invalid syntax!!!")
                .targetModel("model-1")
                .priority(1)
                .build();

            strategy.setRules(List.of(invalidRule));

            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }

        @Test
        @DisplayName("规则匹配但目标模型不存在抛出异常")
        void testRuleMatchesButTargetModelNotFound() {
            RouterRule rule = RouterRule.builder()
                .name("always-match")
                .condition("true")
                .targetModel("non-existent-model")
                .priority(1)
                .build();

            strategy.setRules(List.of(rule));

            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }

        @Test
        @DisplayName("规则匹配但目标模型不可用抛出异常")
        void testRuleMatchesButTargetModelUnavailable() {
            RouterRule rule = RouterRule.builder()
                .name("always-match")
                .condition("true")
                .targetModel("model-1")
                .priority(1)
                .build();

            strategy.setRules(List.of(rule));

            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.UNHEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }

        @Test
        @DisplayName("null 规则列表设置为空列表")
        void testNullRulesList() {
            strategy.setRules(null);

            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }

        @Test
        @DisplayName("SpEL 表达式返回非布尔值时跳过规则")
        void testSpelReturnsNonBoolean() {
            RouterRule rule = RouterRule.builder()
                .name("string-return")
                .condition("'test'")
                .targetModel("model-1")
                .priority(1)
                .build();

            strategy.setRules(List.of(rule));

            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }
    }

    @Nested
    @DisplayName("WeightedRouterStrategy 异常测试")
    class WeightedRouterStrategyExceptionTests {

        private final WeightedRouterStrategy strategy = new WeightedRouterStrategy();

        @Test
        @DisplayName("空候选列表抛出异常")
        void testEmptyCandidateList() {
            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), new ArrayList<>());
            });
        }

        @Test
        @DisplayName("所有模型权重为0时仍可路由")
        void testAllZeroWeights() {
            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 1, HealthStatus.HEALTHY),
                createModelInstance("model-2", "test", 1, HealthStatus.HEALTHY)
            );

            assertDoesNotThrow(() -> strategy.select(new Prompt(new UserMessage("test")), candidates));
        }

        @Test
        @DisplayName("所有模型不可用抛出异常")
        void testAllUnavailable() {
            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.UNHEALTHY),
                createModelInstance("model-2", "test", 50, HealthStatus.UNHEALTHY)
            );

            assertThrows(ModelUnavailableException.class, () -> {
                strategy.select(new Prompt(new UserMessage("test")), candidates);
            });
        }
    }
}
