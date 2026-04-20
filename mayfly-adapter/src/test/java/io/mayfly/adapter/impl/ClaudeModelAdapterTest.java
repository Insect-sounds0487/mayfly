package io.mayfly.adapter.impl;

import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClaudeModelAdapter 单元测试")
class ClaudeModelAdapterTest {
    
    private final ClaudeModelAdapter adapter = new ClaudeModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("claude", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-test")
                .provider("claude")
                .model("claude-3-5-sonnet-20241022")
                .apiKey("sk-ant-test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-full")
                .provider("claude")
                .model("claude-3-opus-20240229")
                .apiKey("sk-ant-test-api-key")
                .baseUrl("https://api.anthropic.com")
                .weight(40)
                .timeout(90000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-null-key")
                .provider("claude")
                .model("claude-3-5-sonnet-20241022")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时抛出异常")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-empty-key")
                .provider("claude")
                .model("claude-3-5-sonnet-20241022")
                .apiKey("")
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-default-model")
                .provider("claude")
                .model(null)
                .apiKey("sk-ant-test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-default-url")
                .provider("claude")
                .model("claude-3-5-sonnet-20241022")
                .apiKey("sk-ant-test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("claude-custom-url")
                .provider("claude")
                .model("claude-3-5-sonnet-20241022")
                .apiKey("sk-ant-test-api-key")
                .baseUrl("https://proxy.anthropic.api")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
    }
}
