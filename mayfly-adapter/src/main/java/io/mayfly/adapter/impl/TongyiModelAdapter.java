package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * 阿里通义千问 模型适配器
 * 支持 Qwen-Max, Qwen-Plus, Qwen-Turbo 等模型
 * 
 * 通义千问API兼容OpenAI格式，可通过DashScope OpenAI兼容接口调用
 */
@Component
public class TongyiModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "tongyi";
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Tongyi API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "qwen-max";
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
