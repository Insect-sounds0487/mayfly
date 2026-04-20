package io.mayfly.adapter.impl;

import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatModel 异常处理测试
 */
@DisplayName("ChatModel 异常处理测试")
class ChatModelExceptionTest {

    private ZhipuModelAdapter.ZhipuChatModel createChatModel(HttpClient mockClient) {
        ZhipuModelAdapter adapter = new ZhipuModelAdapter() {
            @Override
            protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
                return mockClient;
            }
        };

        return (ZhipuModelAdapter.ZhipuChatModel) adapter.createChatModel(ModelConfig.builder()
            .name("test-model")
            .provider("zhipu")
            .model("glm-4")
            .apiKey("test-key")
            .build());
    }

    @Nested
    @DisplayName("响应格式异常测试")
    class ResponseFormatErrorTests {

        @Test
        @DisplayName("空 choices 列表")
        void testEmptyChoicesList() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-123",
                "choices", List.of()
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            assertThrows(IndexOutOfBoundsException.class, () -> chatModel.call(prompt));
        }

        @Test
        @DisplayName("choices 为 null")
        void testNullChoices() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-123"
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            assertThrows(NullPointerException.class, () -> chatModel.call(prompt));
        }

        @Test
        @DisplayName("message 字段缺失")
        void testMissingMessageField() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-123",
                "choices", List.of(Map.of(
                    "index", 0,
                    "finish_reason", "stop"
                ))
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            assertThrows(NullPointerException.class, () -> chatModel.call(prompt));
        }

        @Test
        @DisplayName("content 字段为 null")
        void testNullContent() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-123",
                "choices", List.of(Map.of(
                    "index", 0,
                    "message", Map.of("role", "assistant"),
                    "finish_reason", "stop"
                ))
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

            assertNotNull(response);
            assertNull(response.getResult().getOutput().getText());
        }

        @Test
        @DisplayName("content 为空字符串")
        void testEmptyContent() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-123",
                "choices", List.of(Map.of(
                    "index", 0,
                    "message", Map.of("role", "assistant", "content", ""),
                    "finish_reason", "stop"
                ))
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

            assertNotNull(response);
            assertEquals("", response.getResult().getOutput().getText());
        }
    }

    @Nested
    @DisplayName("错误响应处理测试")
    class ErrorResponseTests {

        @Test
        @DisplayName("API 返回错误信息")
        void testApiErrorMessage() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "error", Map.of(
                    "message", "Invalid model parameter",
                    "type", "invalid_request_error",
                    "code", "invalid_model"
                )
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            assertThrows(NullPointerException.class, () -> chatModel.call(prompt));
        }

        @Test
        @DisplayName("响应包含非预期数据结构")
        void testUnexpectedResponseStructure() {
            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "data", "unexpected format",
                "status", "ok"
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            assertThrows(NullPointerException.class, () -> chatModel.call(prompt));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("处理超长响应内容")
        void testVeryLongResponseContent() {
            String longContent = "A".repeat(10000);

            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-long",
                "choices", List.of(Map.of(
                    "index", 0,
                    "message", Map.of("role", "assistant", "content", longContent),
                    "finish_reason", "stop"
                ))
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

            assertNotNull(response);
            assertEquals(10000, response.getResult().getOutput().getText().length());
        }

        @Test
        @DisplayName("处理特殊字符响应")
        void testSpecialCharactersInResponse() {
            String specialContent = "Hello <>&\"' 你好 🎉";

            HttpClient mockClient = (url, headers, requestBody) -> Map.of(
                "id", "chatcmpl-special",
                "choices", List.of(Map.of(
                    "index", 0,
                    "message", Map.of("role", "assistant", "content", specialContent),
                    "finish_reason", "stop"
                ))
            );

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(
                    new org.springframework.ai.chat.messages.UserMessage("Hello"));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

            assertNotNull(response);
            assertEquals(specialContent, response.getResult().getOutput().getText());
        }

        @Test
        @DisplayName("处理多轮对话助手消息")
        void testAssistantMessageInPrompt() {
            HttpClient mockClient = (url, headers, requestBody) -> {
                @SuppressWarnings("unchecked")
                List<Object> messages = (List<Object>) getPropertyValue(requestBody, "messages");
                assertEquals(3, messages.size());
                assertEquals("user", getPropertyValue(messages.get(0), "role"));
                assertEquals("assistant", getPropertyValue(messages.get(1), "role"));
                assertEquals("user", getPropertyValue(messages.get(2), "role"));

                return Map.of(
                    "id", "chatcmpl-multiturn",
                    "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", "Follow-up response"),
                        "finish_reason", "stop"
                    ))
                );
            };

            ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModel(mockClient);

            org.springframework.ai.chat.prompt.Prompt prompt =
                new org.springframework.ai.chat.prompt.Prompt(List.of(
                    new org.springframework.ai.chat.messages.UserMessage("First question"),
                    new org.springframework.ai.chat.messages.AssistantMessage("First answer"),
                    new org.springframework.ai.chat.messages.UserMessage("Follow-up question")
                ));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);

            assertNotNull(response);
            assertEquals("Follow-up response", response.getResult().getOutput().getText());
        }

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
    }
}
