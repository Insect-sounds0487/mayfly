package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * OpenAI 模型适配器
 * 支持 GPT-4, GPT-3.5-turbo 等模型
 */
@Component
public class OpenAiModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "openai";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("OpenAI API Key cannot be null or empty");
        }
        
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank() 
            ? config.getBaseUrl() 
            : DEFAULT_BASE_URL;
        
        OpenAiApi openAiApi = new OpenAiApi(config.getApiKey(), baseUrl);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(config.getModel() != null ? config.getModel() : "gpt-4")
            .temperature(0.7)
            .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER;
    }
}
