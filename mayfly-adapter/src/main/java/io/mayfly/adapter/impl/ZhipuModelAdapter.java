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
 * 智谱 AI (ZhiPuAI) 自定义适配器
 * 支持 GLM-4 系列模型
 */
@Component
public class ZhipuModelAdapter implements ModelAdapter {

    private static final String PROVIDER = "zhipu";
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    @Override
    public ChatModel createChatModel(ModelConfig config) {
        Assert.notNull(config.getApiKey(), "Zhipu API Key must not be null");
        String model = config.getModel() != null ? config.getModel() : "glm-4";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank()
            ? config.getBaseUrl()
            : DEFAULT_BASE_URL;

        return new ZhipuChatModel(config.getApiKey(), baseUrl, model, createHttpClient(config.getApiKey(), baseUrl, model));
    }

    protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
        return new ZhipuHttpClient(apiKey, baseUrl, model);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * 智谱 AI 聊天模型实现
     */
    static class ZhipuChatModel implements ChatModel {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final HttpClient httpClient;

        public ZhipuChatModel(String apiKey, String baseUrl, String model, HttpClient httpClient) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.httpClient = httpClient;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ZhipuChatRequest request = buildRequest(prompt);
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

        private ZhipuChatRequest buildRequest(Prompt prompt) {
            ZhipuChatRequest request = new ZhipuChatRequest();
            request.setModel(model);
            request.setMessages(convertMessages(prompt.getInstructions()));
            request.setTemperature(0.7);
            request.setMaxTokens(2048);
            return request;
        }

        private List<ZhipuChatMessage> convertMessages(List<Message> messages) {
            List<ZhipuChatMessage> zhipuMessages = new ArrayList<>();

            for (Message message : messages) {
                ZhipuChatMessage zhipuMessage = new ZhipuChatMessage();

                if (message instanceof UserMessage) {
                    zhipuMessage.setRole("user");
                    zhipuMessage.setContent(message.getText());
                } else if (message instanceof SystemMessage) {
                    zhipuMessage.setRole("system");
                    zhipuMessage.setContent(message.getText());
                } else if (message instanceof AssistantMessage) {
                    zhipuMessage.setRole("assistant");
                    zhipuMessage.setContent(message.getText());
                }

                zhipuMessages.add(zhipuMessage);
            }

            return zhipuMessages;
        }
    }

    /**
     * 智谱 AI HTTP 客户端实现
     */
    private static class ZhipuHttpClient implements HttpClient {
            private final String apiKey;
            private final String baseUrl;
            private final String model;

            public ZhipuHttpClient(String apiKey, String baseUrl, String model) {
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
     * 智谱 AI 请求消息格式
     */
    static class ZhipuChatMessage {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 智谱 AI 聊天请求格式
     */
    static class ZhipuChatRequest {
        private String model;
        private List<ZhipuChatMessage> messages;
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<ZhipuChatMessage> getMessages() { return messages; }
        public void setMessages(List<ZhipuChatMessage> messages) { this.messages = messages; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }
}
