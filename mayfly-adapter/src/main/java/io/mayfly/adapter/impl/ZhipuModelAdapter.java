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
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 智谱 AI (ZhiPuAI) 自定义适配器
 * 支持 GLM-4 系列模型
 */
@Component
public class ZhipuModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        return new ZhipuChatModel(config);
    }
    
    @Override
    public String getProvider() {
        return "zhipu";
    }
    
    /**
     * 智谱 AI 聊天模型实现
     */
    private static class ZhipuChatModel extends BaseHttpClient implements ChatModel {
        
        public ZhipuChatModel(ModelConfig config) {
            super(
                config.getApiKey(),
                config.getBaseUrl() != null ? config.getBaseUrl() : "https://open.bigmodel.cn/api/paas/v4",
                config.getModel() != null ? config.getModel() : "glm-4"
            );
        }
        
        @Override
        public ChatResponse call(Prompt prompt) {
            // 构建请求体
            ZhipuChatRequest request = buildRequest(prompt);
            
            // 发送请求
            ZhipuChatResponse response = restTemplate.postForObject(
                baseUrl + "/chat/completions",
                createRequestEntity(request),
                ZhipuChatResponse.class
            );
            
            Assert.notNull(response, "智谱 AI 响应为空");
            Assert.notEmpty(response.getChoices(), "智谱 AI 响应中没有 choices");
            
            // 转换为 Spring AI 格式
            AssistantMessage assistantMessage = new AssistantMessage(
                response.getChoices().get(0).getMessage().getContent()
            );
            Generation generation = new Generation(assistantMessage);
            
            return new ChatResponse(List.of(generation));
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
     * 智谱 AI 请求消息格式
     */
    private static class ZhipuChatMessage {
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
    private static class ZhipuChatRequest {
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
    
    /**
     * 智谱 AI 聊天响应格式
     */
    private static class ZhipuChatResponse {
        private String id;
        private Long created;
        private String model;
        private List<ZhipuChatChoice> choices;
        private ZhipuUsage usage;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getCreated() { return created; }
        public void setCreated(Long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<ZhipuChatChoice> getChoices() { return choices; }
        public void setChoices(List<ZhipuChatChoice> choices) { this.choices = choices; }
        public ZhipuUsage getUsage() { return usage; }
        public void setUsage(ZhipuUsage usage) { this.usage = usage; }
    }
    
    private static class ZhipuChatChoice {
        private Integer index;
        private ZhipuChatMessage message;
        private String finishReason;
        
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public ZhipuChatMessage getMessage() { return message; }
        public void setMessage(ZhipuChatMessage message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }
    
    private static class ZhipuUsage {
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