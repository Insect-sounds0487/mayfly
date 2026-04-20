package io.mayfly.adapter.impl;

import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeepSeekModelAdapter 单元测试")
class DeepSeekModelAdapterTest {
    
    private final DeepSeekModelAdapter adapter = new DeepSeekModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("deepseek", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-test")
                .provider("deepseek")
                .model("deepseek-chat")
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-full")
                .provider("deepseek")
                .model("deepseek-reasoner")
                .apiKey("test-api-key")
                .baseUrl("https://custom.deepseek.api")
                .weight(60)
                .timeout(30000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-null-key")
                .provider("deepseek")
                .model("deepseek-chat")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时使用默认值")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-empty-key")
                .provider("deepseek")
                .model("deepseek-chat")
                .apiKey("")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-default-model")
                .provider("deepseek")
                .model(null)
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-default-url")
                .provider("deepseek")
                .model("deepseek-chat")
                .apiKey("test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("deepseek-custom-url")
                .provider("deepseek")
                .model("deepseek-chat")
                .apiKey("test-api-key")
                .baseUrl("https://private.deepseek.com/v1")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
    }
    
    @Nested
    @DisplayName("Mock HTTP 调用测试")
    class MockHttpCallTests {
        
        private Object getPropertyValue(Object obj, String propertyName) {
            try {
                String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                java.lang.reflect.Method method = obj.getClass().getDeclaredMethod(getterName);
                method.setAccessible(true);
                return method.invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get property: " + propertyName, e);
            }
        }
        
        private DeepSeekModelAdapter createMockAdapter(HttpClient mockClient) {
            return new DeepSeekModelAdapter() {
                @Override
                protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
                    return mockClient;
                }
            };
        }
        
        @Test
        @DisplayName("测试成功调用返回响应内容")
        void testSuccessfulCallReturnsContent() {
            HttpClient mockClient = (url, headers, requestBody) -> {
                return Map.of(
                    "id", "chatcmpl-deepseek-123",
                    "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "Hello from DeepSeek!"),
                        "finish_reason", "stop"
                    ))
                );
            };
            
            DeepSeekModelAdapter mockAdapter = createMockAdapter(mockClient);
            DeepSeekModelAdapter.DeepSeekChatModel chatModel = (DeepSeekModelAdapter.DeepSeekChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("deepseek-mock")
                    .provider("deepseek")
                    .model("deepseek-chat")
                    .apiKey("test-key")
                    .build());
            
            org.springframework.ai.chat.prompt.Prompt prompt = 
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));
            
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            
            assertNotNull(response);
            assertEquals(1, response.getResults().size());
            assertEquals("Hello from DeepSeek!", response.getResult().getOutput().getText());
        }
        
        @Test
        @DisplayName("测试多轮对话消息转换")
        void testMultiTurnMessageConversion() {
            HttpClient mockClient = (url, headers, requestBody) -> {
                assertEquals("deepseek-chat", getPropertyValue(requestBody, "model"));
                assertEquals(0.7, getPropertyValue(requestBody, "temperature"));
                assertEquals(2048, getPropertyValue(requestBody, "maxTokens"));
                
                @SuppressWarnings("unchecked")
                List<Object> messages = (List<Object>) getPropertyValue(requestBody, "messages");
                assertEquals(2, messages.size());
                assertEquals("system", getPropertyValue(messages.get(0), "role"));
                assertEquals("user", getPropertyValue(messages.get(1), "role"));
                
                return Map.of(
                    "id", "chatcmpl-deepseek-456",
                    "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "Multi-turn response"),
                        "finish_reason", "stop"
                    ))
                );
            };
            
            DeepSeekModelAdapter mockAdapter = createMockAdapter(mockClient);
            DeepSeekModelAdapter.DeepSeekChatModel chatModel = (DeepSeekModelAdapter.DeepSeekChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("deepseek-multiturn")
                    .provider("deepseek")
                    .model("deepseek-chat")
                    .apiKey("test-key")
                    .build());
            
            org.springframework.ai.chat.prompt.Prompt prompt = 
                new org.springframework.ai.chat.prompt.Prompt(List.of(
                    new org.springframework.ai.chat.messages.SystemMessage("You are a helpful assistant"),
                    new org.springframework.ai.chat.messages.UserMessage("Tell me a joke")
                ));
            
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            
            assertNotNull(response);
            assertEquals("Multi-turn response", response.getResult().getOutput().getText());
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl的请求URL")
        void testCustomBaseUrlInRequest() {
            final String[] capturedUrl = new String[1];
            
            HttpClient mockClient = (url, headers, requestBody) -> {
                capturedUrl[0] = url;
                return Map.of(
                    "choices", List.of(Map.of(
                        "message", Map.of("content", "Response from custom URL")
                    ))
                );
            };
            
            DeepSeekModelAdapter mockAdapter = createMockAdapter(mockClient);
            DeepSeekModelAdapter.DeepSeekChatModel chatModel = (DeepSeekModelAdapter.DeepSeekChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("deepseek-custom")
                    .provider("deepseek")
                    .model("deepseek-chat")
                    .apiKey("test-key")
                    .baseUrl("https://custom.api.cn")
                    .build());
            
            org.springframework.ai.chat.prompt.Prompt prompt = 
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Test"));
            
            chatModel.call(prompt);
            
            assertEquals("https://custom.api.cn/chat/completions", capturedUrl[0]);
        }
        
        @Test
        @DisplayName("测试请求头包含正确的认证信息")
        void testRequestHeadersContainAuth() {
            final Map<String, Object>[] capturedHeaders = new Map[1];
            
            HttpClient mockClient = (url, headers, requestBody) -> {
                capturedHeaders[0] = headers;
                return Map.of(
                    "choices", List.of(Map.of(
                        "message", Map.of("content", "Auth test response")
                    ))
                );
            };
            
            DeepSeekModelAdapter mockAdapter = createMockAdapter(mockClient);
            DeepSeekModelAdapter.DeepSeekChatModel chatModel = (DeepSeekModelAdapter.DeepSeekChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("deepseek-auth")
                    .provider("deepseek")
                    .model("deepseek-chat")
                    .apiKey("my-secret-key")
                    .build());
            
            org.springframework.ai.chat.prompt.Prompt prompt = 
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Test"));
            
            chatModel.call(prompt);
            
            assertEquals("Bearer my-secret-key", capturedHeaders[0].get("Authorization"));
            assertEquals("application/json", capturedHeaders[0].get("Content-Type"));
        }
    }
}
