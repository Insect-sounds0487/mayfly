package io.mayfly.failover;

import io.mayfly.core.HealthStatus;
import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * FailoverHandler 单元测试
 */
class FailoverHandlerTest {
    
    private FailoverHandler failoverHandler;
    private ModelInstance primaryModel;
    private ModelInstance backupModel1;
    private ModelInstance backupModel2;
    private List<ModelInstance> candidates;
    private Prompt request;
    
    @BeforeEach
    void setUp() {
        FailoverConfig config = FailoverConfig.builder()
            .enabled(true)
            .maxRetries(2)
            .cooldownDuration(Duration.ofSeconds(30))
            .retryableExceptions(Arrays.asList(
                "java.net.SocketTimeoutException",
                "org.springframework.web.client.HttpServerErrorException"
            ))
            .build();
        
        failoverHandler = new FailoverHandler(config);
        
        ChatModel mockChatModel = mock(ChatModel.class);
        
        ModelConfig primaryConfig = ModelConfig.builder()
            .name("primary-model")
            .provider("zhipu")
            .model("glm-4")
            .weight(70)
            .build();
        primaryModel = new ModelInstance(primaryConfig, mockChatModel);
        
        ModelConfig backupConfig1 = ModelConfig.builder()
            .name("backup-model-1")
            .provider("tongyi")
            .model("qwen-max")
            .weight(30)
            .build();
        backupModel1 = new ModelInstance(backupConfig1, mockChatModel);
        
        ModelConfig backupConfig2 = ModelConfig.builder()
            .name("backup-model-2")
            .provider("deepseek")
            .model("deepseek-coder")
            .weight(20)
            .build();
        backupModel2 = new ModelInstance(backupConfig2, mockChatModel);
        
        candidates = Arrays.asList(primaryModel, backupModel1, backupModel2);
        
        request = new Prompt(new UserMessage("Test message"));
    }
    
    @Test
    void testExecuteFailover_Success() {
        Exception exception = new SocketTimeoutException("Connection timeout");
        
        FailoverResult result = failoverHandler.executeFailover(
            request, primaryModel, candidates, exception);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getTargetModel());
        assertNotEquals(primaryModel.getConfig().getName(), 
            result.getTargetModel().getConfig().getName());
        assertNull(result.getErrorMessage());
        
        assertEquals(HealthStatus.COOLDOWN, primaryModel.getHealthStatus());
        assertNotNull(primaryModel.getCooldownUntil());
    }
    
    @Test
    void testExecuteFailover_NoBackupAvailable() {
        List<ModelInstance> onlyPrimary = Collections.singletonList(primaryModel);
        Exception exception = new SocketTimeoutException("Connection timeout");
        
        FailoverResult result = failoverHandler.executeFailover(
            request, primaryModel, onlyPrimary, exception);
        
        assertFalse(result.isSuccess());
        assertNull(result.getTargetModel());
        assertEquals("No backup available", result.getErrorMessage());
    }
    
    @Test
    void testExecuteFailover_SkipUnavailableBackups() {
        backupModel1.getConfig().setEnabled(false);
        backupModel2.setHealthStatus(HealthStatus.UNHEALTHY);
        
        List<ModelInstance> limitedCandidates = Arrays.asList(
            primaryModel, backupModel1, backupModel2);
        Exception exception = new SocketTimeoutException("Connection timeout");
        
        FailoverResult result = failoverHandler.executeFailover(
            request, primaryModel, limitedCandidates, exception);
        
        assertFalse(result.isSuccess());
        assertEquals("No backup available", result.getErrorMessage());
    }
    
    @Test
    void testExecuteFailover_SelectFirstAvailableBackup() {
        Exception exception = new SocketTimeoutException("Connection timeout");
        
        FailoverResult result = failoverHandler.executeFailover(
            request, primaryModel, candidates, exception);
        
        assertTrue(result.isSuccess());
        assertEquals("backup-model-1", 
            result.getTargetModel().getConfig().getName());
    }
    
    @Test
    void testIsRetryable_SocketTimeoutException() {
        Exception exception = new SocketTimeoutException("Timeout");
        assertTrue(failoverHandler.isRetryable(exception));
    }
    
    @Test
    void testIsRetryable_NonRetryableException() {
        Exception exception = new RuntimeException("Unknown error");
        assertFalse(failoverHandler.isRetryable(exception));
    }
    
    @Test
    void testIsRetryable_IllegalArgumentException() {
        Exception exception = new IllegalArgumentException("Invalid argument");
        assertFalse(failoverHandler.isRetryable(exception));
    }
    
    @Test
    void testDefaultConstructor() {
        FailoverHandler defaultHandler = new FailoverHandler();
        assertNotNull(defaultHandler);
        
        Exception exception = new SocketTimeoutException("Timeout");
        assertTrue(defaultHandler.isRetryable(exception));
    }
    
    @Test
    void testFailoverConfig_DefaultValues() {
        FailoverConfig defaultConfig = FailoverConfig.builder().build();
        
        assertTrue(defaultConfig.isEnabled());
        assertEquals(2, defaultConfig.getMaxRetries());
        assertEquals(Duration.ofSeconds(60), defaultConfig.getCooldownDuration());
        assertFalse(defaultConfig.getRetryableExceptions().isEmpty());
    }
    
    @Test
    void testFailoverResult_Success() {
        FailoverResult result = FailoverResult.success(backupModel1);
        
        assertTrue(result.isSuccess());
        assertEquals(backupModel1, result.getTargetModel());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void testFailoverResult_Failure() {
        FailoverResult result = FailoverResult.failure("Test error");
        
        assertFalse(result.isSuccess());
        assertNull(result.getTargetModel());
        assertEquals("Test error", result.getErrorMessage());
    }
}
