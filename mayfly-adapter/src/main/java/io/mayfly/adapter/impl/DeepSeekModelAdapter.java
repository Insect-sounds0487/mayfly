package io.mayfly.adapter.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.mayfly.adapter.ModelAdapter;
import io.mayfly.adapter.http.BaseHttpClient;
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

        return new DeepSeekChatModel(config.getApiKey(), baseUrl, model);
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    /**
     * DeepSeek 聊天模型实现
     */
    private static class DeepSeekChatModel implements ChatModel {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final BaseHttpClient httpClient;

        public DeepSeekChatModel(String apiKey, String baseUrl, String model) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.httpClient = new BaseHttpClient();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            DeepSeekChatRequest request = buildRequest(prompt);
            String url = baseUrl + "/chat/completions";

            java.util.Map<String, Object> headers = java.util.Map.of(
                "Authorization", "Bearer " + apiKey,
                "Content-Type", "application/json"
            );

            java.util.Map<String, Object> responseData = httpClient.post(url, headers, request, java.util.Map.class);

            // 解析响应
            List<java.util.Map<String, Object>> choices = (List<java.util.Map<String, Object>>) responseData.get("choices");
            java.util.Map<String, Object> firstChoice = choices.get(0);
            java.util.Map<String, Object> message = (java.util.Map<String, Object>) firstChoice.get("message");
            String content = (String) message.get("content");

            List<Generation> generations = List.of(new Generation(content));
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
     * DeepSeek 请求消息格式
     */
    private static class DeepSeekChatMessage {
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
    private static class DeepSeekChatRequest {
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

    /**
     * DeepSeek 聊天响应格式
     */
    private static class DeepSeekChatResponse {
        private String id;
        private Long created;
        private String model;
        private List<DeepSeekChatChoice> choices;
        private DeepSeekUsage usage;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getCreated() { return created; }
        public void setCreated(Long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<DeepSeekChatChoice> getChoices() { return choices; }
        public void setChoices(List<DeepSeekChatChoice> choices) { this.choices = choices; }
        public DeepSeekUsage getUsage() { return usage; }
        public void setUsage(DeepSeekUsage usage) { this.usage = usage; }
    }

    private static class DeepSeekChatChoice {
        private Integer index;
        private DeepSeekChatMessage message;
        private String finishReason;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public DeepSeekChatMessage getMessage() { return message; }
        public void setMessage(DeepSeekChatMessage message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    private static class DeepSeekUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    }
}