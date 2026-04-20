package io.mayfly.failover;

import io.mayfly.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Failover 模块异常测试
 */
@DisplayName("Failover 模块异常测试")
class FailoverExceptionTest {

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
    @DisplayName("故障转移异常测试")
    class FailoverExceptionTests {

        private FailoverHandler handler;

        @BeforeEach
        void setUp() {
            FailoverConfig config = FailoverConfig.builder()
                .cooldownDuration(Duration.ofSeconds(30))
                .retryableExceptions(List.of(
                    "java.net.SocketTimeoutException",
                    "java.io.IOException",
                    "org.springframework.web.client.ResourceAccessException"
                ))
                .build();
            handler = new FailoverHandler(config);
        }

        @Test
        @DisplayName("无备用模型时返回失败")
        void testNoBackupModels() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(prompt, failedModel, candidates, exception);

            assertFalse(result.isSuccess());
            assertEquals("No backup available", result.getErrorMessage());
        }

        @Test
        @DisplayName("空候选列表返回失败")
        void testEmptyCandidateList() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = new ArrayList<>();
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(prompt, failedModel, candidates, exception);

            assertFalse(result.isSuccess());
            assertEquals("No backup available", result.getErrorMessage());
        }

        @Test
        @DisplayName("备用模型不可用时返回失败")
        void testBackupModelUnavailable() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backupModel = createModelInstance("model-2", "test", 50, HealthStatus.UNHEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backupModel);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(prompt, failedModel, candidates, exception);

            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("故障模型进入冷却状态")
        void testFailedModelEntersCooldown() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backupModel = createModelInstance("model-2", "test", 50, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backupModel);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            handler.executeFailover(prompt, failedModel, candidates, exception);

            assertEquals(HealthStatus.COOLDOWN, failedModel.getHealthStatus());
            assertNotNull(failedModel.getCooldownUntil());
            assertTrue(failedModel.getCooldownUntil().isAfter(java.time.Instant.now()));
        }

        @Test
        @DisplayName("成功故障转移返回备用模型")
        void testSuccessfulFailover() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backupModel = createModelInstance("model-2", "test", 50, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backupModel);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(prompt, failedModel, candidates, exception);

            assertTrue(result.isSuccess());
            assertEquals("model-2", result.getTargetModel().getConfig().getName());
        }

        @Test
        @DisplayName("多个备用模型时选择第一个可用")
        void testMultipleBackupModels() {
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backup1 = createModelInstance("model-2", "test", 50, HealthStatus.HEALTHY);
            ModelInstance backup2 = createModelInstance("model-3", "test", 30, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backup1, backup2);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(prompt, failedModel, candidates, exception);

            assertTrue(result.isSuccess());
            assertEquals("model-2", result.getTargetModel().getConfig().getName());
        }
    }

    @Nested
    @DisplayName("可重试异常判断测试")
    class RetryableExceptionTests {

        private FailoverHandler handler;

        @BeforeEach
        void setUp() {
            FailoverConfig config = FailoverConfig.builder()
                .cooldownDuration(Duration.ofSeconds(30))
                .retryableExceptions(List.of(
                    "java.net.SocketTimeoutException",
                    "java.io.IOException",
                    "org.springframework.web.client.ResourceAccessException"
                ))
                .build();
            handler = new FailoverHandler(config);
        }

        @Test
        @DisplayName("SocketTimeoutException 可重试")
        void testSocketTimeoutIsRetryable() {
            assertTrue(handler.isRetryable(new java.net.SocketTimeoutException()));
        }

        @Test
        @DisplayName("IOException 可重试")
        void testIOExceptionIsRetryable() {
            assertTrue(handler.isRetryable(new java.io.IOException()));
        }

        @Test
        @DisplayName("RuntimeException 不可重试")
        void testRuntimeExceptionNotRetryable() {
            assertFalse(handler.isRetryable(new RuntimeException()));
        }

        @Test
        @DisplayName("NullPointerException 不可重试")
        void testNullPointerExceptionNotRetryable() {
            assertFalse(handler.isRetryable(new NullPointerException()));
        }

        @Test
        @DisplayName("IllegalArgumentException 不可重试")
        void testIllegalArgumentExceptionNotRetryable() {
            assertFalse(handler.isRetryable(new IllegalArgumentException()));
        }

        @Test
        @DisplayName("空异常列表时所有异常不可重试")
        void testEmptyRetryableList() {
            FailoverConfig config = FailoverConfig.builder()
                .cooldownDuration(Duration.ofSeconds(30))
                .retryableExceptions(List.of())
                .build();
            FailoverHandler emptyHandler = new FailoverHandler(config);

            assertFalse(emptyHandler.isRetryable(new java.net.SocketTimeoutException()));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("null prompt 正常处理")
        void testNullPrompt() {
            FailoverHandler handler = new FailoverHandler();
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backupModel = createModelInstance("model-2", "test", 50, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backupModel);
            Exception exception = new RuntimeException("Connection timeout");

            FailoverResult result = handler.executeFailover(null, failedModel, candidates, exception);

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("null exception 抛出异常")
        void testNullException() {
            FailoverHandler handler = new FailoverHandler();
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            ModelInstance backupModel = createModelInstance("model-2", "test", 50, HealthStatus.HEALTHY);
            List<ModelInstance> candidates = List.of(failedModel, backupModel);
            Prompt prompt = new Prompt(new UserMessage("test"));

            assertThrows(NullPointerException.class, () -> {
                handler.executeFailover(prompt, failedModel, candidates, null);
            });
        }

        @Test
        @DisplayName("null 失败模型抛出异常")
        void testNullFailedModel() {
            FailoverHandler handler = new FailoverHandler();
            List<ModelInstance> candidates = List.of(
                createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY)
            );
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            assertThrows(NullPointerException.class, () -> {
                handler.executeFailover(prompt, null, candidates, exception);
            });
        }

        @Test
        @DisplayName("null 候选列表抛出异常")
        void testNullCandidateList() {
            FailoverHandler handler = new FailoverHandler();
            ModelInstance failedModel = createModelInstance("model-1", "test", 100, HealthStatus.HEALTHY);
            Prompt prompt = new Prompt(new UserMessage("test"));
            Exception exception = new RuntimeException("Connection timeout");

            assertThrows(NullPointerException.class, () -> {
                handler.executeFailover(prompt, failedModel, null, exception);
            });
        }
    }
}
