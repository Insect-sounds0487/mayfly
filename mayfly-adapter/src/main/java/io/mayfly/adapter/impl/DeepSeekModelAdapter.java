package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 模型适配器
 * 支持 DeepSeek-Chat, DeepSeek-Coder 等模型
 * 
 * DeepSeek API完全兼容OpenAI格式
 */
@Component
public class DeepSeekModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "deepseek";
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("DeepSeek API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "deepseek-chat";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank() 
            ? config.getBaseUrl() 
            : DEFAULT_BASE_URL;
        
        OpenAiApi openAiApi = new OpenAiApi(config.getApiKey(), baseUrl);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(model)
            .temperature(0.7)
            .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER;
    }
}
