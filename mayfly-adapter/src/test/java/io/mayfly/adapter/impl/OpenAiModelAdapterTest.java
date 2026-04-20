package io.mayfly.adapter.impl;

import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpenAiModelAdapter 单元测试")
class OpenAiModelAdapterTest {
    
    private final OpenAiModelAdapter adapter = new OpenAiModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("openai", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-test")
                .provider("openai")
                .model("gpt-4")
                .apiKey("sk-test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-full")
                .provider("openai")
                .model("gpt-4-turbo")
                .apiKey("sk-test-api-key")
                .baseUrl("https://api.openai.com/v1")
                .weight(50)
                .timeout(60000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-null-key")
                .provider("openai")
                .model("gpt-4")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时抛出异常")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-empty-key")
                .provider("openai")
                .model("gpt-4")
                .apiKey("")
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-default-model")
                .provider("openai")
                .model(null)
                .apiKey("sk-test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-default-url")
                .provider("openai")
                .model("gpt-4")
                .apiKey("sk-test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("openai-custom-url")
                .provider("openai")
                .model("gpt-4")
                .apiKey("sk-test-api-key")
                .baseUrl("https://proxy.openai.api/v1")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
    }
}
