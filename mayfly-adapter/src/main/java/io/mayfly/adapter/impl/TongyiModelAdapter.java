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
 * 通义千问 (DashScope) 自定义适配器
 * 支持 Qwen-Max 系列模型
 */
@Component
public class TongyiModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        return new TongyiChatModel(config);
    }
    
    @Override
    public String getProvider() {
        return "tongyi";
    }
    
    /**
     * 通义千问聊天模型实现
     */
    private static class TongyiChatModel extends BaseHttpClient implements ChatModel {
        
        public TongyiChatModel(ModelConfig config) {
            super(
                config.getApiKey(),
                config.getBaseUrl() != null ? config.getBaseUrl() : "https://dashscope.aliyuncs.com/compatible-mode/v1",
                config.getModel() != null ? config.getModel() : "qwen-max"
            );
        }
        
        @Override
        public ChatResponse call(Prompt prompt) {
            // 构建请求体
            TongyiChatRequest request = buildRequest(prompt);
            
            // 发送请求
            TongyiChatResponse response = restTemplate.postForObject(
                baseUrl + "/chat/completions",
                createRequestEntity(request),
                TongyiChatResponse.class
            );
            
            Assert.notNull(response, "通义千问响应为空");
            Assert.notEmpty(response.getChoices(), "通义千问响应中没有 choices");
            
            // 转换为 Spring AI 格式
            AssistantMessage assistantMessage = new AssistantMessage(
                response.getChoices().get(0).getMessage().getContent()
            );
            Generation generation = new Generation(assistantMessage);
            
            return new ChatResponse(List.of(generation));
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
     * 通义千问请求消息格式
     */
    private static class TongyiChatMessage {
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
    private static class TongyiChatRequest {
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
    
    /**
     * 通义千问聊天响应格式
     */
    private static class TongyiChatResponse {
        private String id;
        private Long created;
        private String model;
        private List<TongyiChatChoice> choices;
        private TongyiUsage usage;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getCreated() { return created; }
        public void setCreated(Long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<TongyiChatChoice> getChoices() { return choices; }
        public void setChoices(List<TongyiChatChoice> choices) { this.choices = choices; }
        public TongyiUsage getUsage() { return usage; }
        public void setUsage(TongyiUsage usage) { this.usage = usage; }
    }
    
    private static class TongyiChatChoice {
        private Integer index;
        private TongyiChatMessage message;
        private String finishReason;
        
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public TongyiChatMessage getMessage() { return message; }
        public void setMessage(TongyiChatMessage message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }
    
    private static class TongyiUsage {
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