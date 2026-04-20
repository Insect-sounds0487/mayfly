package io.mayfly.adapter.impl;

import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TongyiModelAdapter 单元测试")
class TongyiModelAdapterTest {
    
    private final TongyiModelAdapter adapter = new TongyiModelAdapter();
    
    @Test
    @DisplayName("测试提供商名称")
    void testProviderName() {
        assertEquals("tongyi", adapter.getProvider());
    }
    
    @Nested
    @DisplayName("创建ChatModel测试")
    class CreateChatModelTests {
        
        @Test
        @DisplayName("测试使用最小配置创建ChatModel")
        void testCreateChatModelWithMinimalConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-test")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用完整配置创建ChatModel")
        void testCreateChatModelWithFullConfig() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-full")
                .provider("tongyi")
                .model("qwen-max-latest")
                .apiKey("test-api-key")
                .baseUrl("https://custom.dashscope.api")
                .weight(70)
                .timeout(45000)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空时抛出异常")
        void testCreateChatModelWithNullApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-null-key")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey(null)
                .build();
            
            assertThrows(IllegalArgumentException.class, () -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试API Key为空字符串时使用默认值")
        void testCreateChatModelWithEmptyApiKey() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-empty-key")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey("")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定模型时使用默认模型")
        void testCreateChatModelWithDefaultModel() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-default-model")
                .provider("tongyi")
                .model(null)
                .apiKey("test-api-key")
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试未指定baseUrl时使用默认baseUrl")
        void testCreateChatModelWithDefaultBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-default-url")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey("test-api-key")
                .baseUrl(null)
                .build();
            
            assertDoesNotThrow(() -> adapter.createChatModel(config));
        }
        
        @Test
        @DisplayName("测试使用自定义baseUrl")
        void testCreateChatModelWithCustomBaseUrl() {
            ModelConfig config = ModelConfig.builder()
                .name("tongyi-custom-url")
                .provider("tongyi")
                .model("qwen-max")
                .apiKey("test-api-key")
                .baseUrl("https://private.dashscope.aliyuncs.com/compatible-mode/v1")
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
        
        private TongyiModelAdapter createMockAdapter(HttpClient mockClient) {
            return new TongyiModelAdapter() {
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
                    "id", "chatcmpl-tongyi-123",
                    "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "Hello from Tongyi!"),
                        "finish_reason", "stop"
                    ))
                );
            };
            
            TongyiModelAdapter mockAdapter = createMockAdapter(mockClient);
            TongyiModelAdapter.TongyiChatModel chatModel = (TongyiModelAdapter.TongyiChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("tongyi-mock")
                    .provider("tongyi")
                    .model("qwen-max")
                    .apiKey("test-key")
                    .build());
            
            org.springframework.ai.chat.prompt.Prompt prompt = 
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));
            
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            
            assertNotNull(response);
            assertEquals(1, response.getResults().size());
            assertEquals("Hello from Tongyi!", response.getResult().getOutput().getText());
        }
        
        @Test
        @DisplayName("测试多轮对话消息转换")
        void testMultiTurnMessageConversion() {
            HttpClient mockClient = (url, headers, requestBody) -> {
                assertEquals("qwen-max", getPropertyValue(requestBody, "model"));
                assertEquals(0.7, getPropertyValue(requestBody, "temperature"));
                assertEquals(2048, getPropertyValue(requestBody, "maxTokens"));
                
                @SuppressWarnings("unchecked")
                List<Object> messages = (List<Object>) getPropertyValue(requestBody, "messages");
                assertEquals(2, messages.size());
                assertEquals("system", getPropertyValue(messages.get(0), "role"));
                assertEquals("user", getPropertyValue(messages.get(1), "role"));
                
                return Map.of(
                    "id", "chatcmpl-tongyi-456",
                    "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "Multi-turn response"),
                        "finish_reason", "stop"
                    ))
                );
            };
            
            TongyiModelAdapter mockAdapter = createMockAdapter(mockClient);
            TongyiModelAdapter.TongyiChatModel chatModel = (TongyiModelAdapter.TongyiChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("tongyi-multiturn")
                    .provider("tongyi")
                    .model("qwen-max")
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
            
            TongyiModelAdapter mockAdapter = createMockAdapter(mockClient);
            TongyiModelAdapter.TongyiChatModel chatModel = (TongyiModelAdapter.TongyiChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("tongyi-custom")
                    .provider("tongyi")
                    .model("qwen-max")
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
            
            TongyiModelAdapter mockAdapter = createMockAdapter(mockClient);
            TongyiModelAdapter.TongyiChatModel chatModel = (TongyiModelAdapter.TongyiChatModel) 
                mockAdapter.createChatModel(ModelConfig.builder()
                    .name("tongyi-auth")
                    .provider("tongyi")
                    .model("qwen-max")
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
