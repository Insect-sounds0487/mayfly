package io.mayfly.adapter.impl;

import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XinghuoModelAdapter 单元测试")
class XinghuoModelAdapterTest {
    
    private final XinghuoModelAdapter adapter = new XinghuoModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("xinghuo", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-test")
                .provider("xinghuo")
                .model("spark-4.0-ultra")
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-full")
                .provider("xinghuo")
                .model("spark-3.5-max")
                .apiKey("test-api-key")
                .baseUrl("https://spark-api-open.xf-yun.com/v1")
                .weight(20)
                .timeout(35000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-null-key")
                .provider("xinghuo")
                .model("spark-4.0-ultra")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时抛出异常")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-empty-key")
                .provider("xinghuo")
                .model("spark-4.0-ultra")
                .apiKey("")
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-default-model")
                .provider("xinghuo")
                .model(null)
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-default-url")
                .provider("xinghuo")
                .model("spark-4.0-ultra")
                .apiKey("test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("xinghuo-custom-url")
                .provider("xinghuo")
                .model("spark-4.0-ultra")
                .apiKey("test-api-key")
                .baseUrl("https://private.xf-yun.com/v1")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
    }
}
