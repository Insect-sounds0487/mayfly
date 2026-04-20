package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * DeepSeek 自定义适配器
 * DeepSeek 兼容 OpenAI API
 */
@Component
public class DeepSeekModelAdapter implements ModelAdapter {

    private static final String PROVIDER = "deepseek";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        Assert.notNull(config.getApiKey(), "DeepSeek API Key must not be null");
        String model = config.getModel() != null ? config.getModel() : "deepseek-chat";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
            ? config.getBaseUrl()
            : DEFAULT_BASE_URL;

        return new DeepSeekChatModel(config.getApiKey(), baseUrl, model, createHttpClient(config.getApiKey(), baseUrl, model));
    }

    protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
        return new DeepSeekHttpClient(apiKey, baseUrl, model);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * DeepSeek 聊天模型实现
     */
    static class DeepSeekChatModel implements ChatModel {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final HttpClient httpClient;

        public DeepSeekChatModel(String apiKey, String baseUrl, String model, HttpClient httpClient) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.httpClient = httpClient;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            DeepSeekChatRequest request = buildRequest(prompt);
            String url = baseUrl + "/chat/completions";

            Map<String, Object> headers = Map.of(
                "Authorization", "Bearer " + apiKey,
                "Content-Type", "application/json"
            );

            Map<String, Object> responseData = (Map<String, Object>) httpClient.post(url, headers, request);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseData.get("choices");
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = (String) message.get("content");

            List<Generation> generations = List.of(new Generation(new AssistantMessage(content)));
            return new ChatResponse(generations);
        }

        private DeepSeekChatRequest buildRequest(Prompt prompt) {
            DeepSeekChatRequest request = new DeepSeekChatRequest();
            request.setModel(model);
            request.setMessages(convertMessages(prompt.getInstructions()));
            request.setTemperature(0.7);
            request.setMaxTokens(2048);
            return request;
        }

        private List<DeepSeekChatMessage> convertMessages(List<Message> messages) {
            List<DeepSeekChatMessage> deepseekMessages = new ArrayList<>();

            for (Message message : messages) {
                DeepSeekChatMessage deepseekMessage = new DeepSeekChatMessage();

                if (message instanceof UserMessage) {
                    deepseekMessage.setRole("user");
                    deepseekMessage.setContent(message.getText());
                } else if (message instanceof SystemMessage) {
                    deepseekMessage.setRole("system");
                    deepseekMessage.setContent(message.getText());
                } else if (message instanceof AssistantMessage) {
                    deepseekMessage.setRole("assistant");
                    deepseekMessage.setContent(message.getText());
                }

                deepseekMessages.add(deepseekMessage);
            }

            return deepseekMessages;
        }
    }

    /**
     * DeepSeek HTTP 客户端实现
     */
    private static class DeepSeekHttpClient implements HttpClient {
        private final String apiKey;
        private final String baseUrl;
        private final String model;

        public DeepSeekHttpClient(String apiKey, String baseUrl, String model) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
        }

        @Override
        public Object post(String url, Map<String, Object> headers, Object requestBody) {
            throw new UnsupportedOperationException("HTTP implementation required");
        }
    }

    /**
     * DeepSeek 请求消息格式
     */
    static class DeepSeekChatMessage {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * DeepSeek 聊天请求格式
     */
    static class DeepSeekChatRequest {
        private String model;
        private List<DeepSeekChatMessage> messages;
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<DeepSeekChatMessage> getMessages() { return messages; }
        public void setMessages(List<DeepSeekChatMessage> messages) { this.messages = messages; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }
}
