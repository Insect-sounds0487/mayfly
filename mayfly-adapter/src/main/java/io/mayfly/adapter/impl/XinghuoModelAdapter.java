package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * 讯飞星火 模型适配器
 * 支持 Spark-4.0, Spark-3.5 等模型
 * 
 * 讯飞星火API兼容OpenAI格式，可通过OpenAI兼容接口调用
 */
@Component
public class XinghuoModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "xinghuo";
    private static final String DEFAULT_BASE_URL = "https://spark-api-open.xf-yun.com/v1";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Xinghuo API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "spark-4.0-ultra";
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
