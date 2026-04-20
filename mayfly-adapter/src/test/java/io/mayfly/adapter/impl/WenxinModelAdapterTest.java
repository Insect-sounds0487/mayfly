package io.mayfly.adapter.impl;

import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WenxinModelAdapter 单元测试")
class WenxinModelAdapterTest {
    
    private final WenxinModelAdapter adapter = new WenxinModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("wenxin", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-test")
                .provider("wenxin")
                .model("ernie-4.0-8k")
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-full")
                .provider("wenxin")
                .model("ernie-4.0-turbo-8k")
                .apiKey("test-api-key")
                .baseUrl("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat")
                .weight(30)
                .timeout(40000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-null-key")
                .provider("wenxin")
                .model("ernie-4.0-8k")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时抛出异常")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-empty-key")
                .provider("wenxin")
                .model("ernie-4.0-8k")
                .apiKey("")
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-default-model")
                .provider("wenxin")
                .model(null)
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-default-url")
                .provider("wenxin")
                .model("ernie-4.0-8k")
                .apiKey("test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("wenxin-custom-url")
                .provider("wenxin")
                .model("ernie-4.0-8k")
                .apiKey("test-api-key")
                .baseUrl("https://private.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
    }
}
