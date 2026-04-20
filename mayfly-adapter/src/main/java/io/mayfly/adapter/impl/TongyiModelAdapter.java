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
 * 通义千问 (DashScope) 自定义适配器
 * 支持 Qwen-Max 系列模型
 */
@Component
public class TongyiModelAdapter implements ModelAdapter {

    private static final String PROVIDER = "tongyi";
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        Assert.notNull(config.getApiKey(), "Tongyi API Key must not be null");
        String model = config.getModel() != null ? config.getModel() : "qwen-max";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
            ? config.getBaseUrl()
            : DEFAULT_BASE_URL;

        return new TongyiChatModel(config.getApiKey(), baseUrl, model, createHttpClient(config.getApiKey(), baseUrl, model));
    }

    protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
        return new TongyiHttpClient(apiKey, baseUrl, model);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * 通义千问聊天模型实现
     */
    static class TongyiChatModel implements ChatModel {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final HttpClient httpClient;

        public TongyiChatModel(String apiKey, String baseUrl, String model, HttpClient httpClient) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.httpClient = httpClient;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            TongyiChatRequest request = buildRequest(prompt);
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

        private TongyiChatRequest buildRequest(Prompt prompt) {
            TongyiChatRequest request = new TongyiChatRequest();
            request.setModel(model);
            request.setMessages(convertMessages(prompt.getInstructions()));
            request.setTemperature(0.7);
            request.setMaxTokens(2048);
            return request;
        }

        private List<TongyiChatMessage> convertMessages(List<Message> messages) {
            List<TongyiChatMessage> tongyiMessages = new ArrayList<>();

            for (Message message : messages) {
                TongyiChatMessage tongyiMessage = new TongyiChatMessage();

                if (message instanceof UserMessage) {
                    tongyiMessage.setRole("user");
                    tongyiMessage.setContent(message.getText());
                } else if (message instanceof SystemMessage) {
                    tongyiMessage.setRole("system");
                    tongyiMessage.setContent(message.getText());
                } else if (message instanceof AssistantMessage) {
                    tongyiMessage.setRole("assistant");
                    tongyiMessage.setContent(message.getText());
                }

                tongyiMessages.add(tongyiMessage);
            }

            return tongyiMessages;
        }
    }

    /**
     * 通义千问 HTTP 客户端实现
     */
    private static class TongyiHttpClient implements HttpClient {
        private final String apiKey;
        private final String baseUrl;
        private final String model;

        public TongyiHttpClient(String apiKey, String baseUrl, String model) {
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
     * 通义千问请求消息格式
     */
    static class TongyiChatMessage {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 通义千问聊天请求格式
     */
    static class TongyiChatRequest {
        private String model;
        private List<TongyiChatMessage> messages;
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<TongyiChatMessage> getMessages() { return messages; }
        public void setMessages(List<TongyiChatMessage> messages) { this.messages = messages; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }
}
