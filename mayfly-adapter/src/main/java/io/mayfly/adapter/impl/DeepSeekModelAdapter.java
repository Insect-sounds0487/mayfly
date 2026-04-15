package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * DeepSeek适配器
 * DeepSeek使用OpenAI兼容的API接口
 */
@Component
public class DeepSeekModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API key must not be empty for provider: deepseek");
        }
        
        String baseUrl = config.getBaseUrl() != null ? 
            config.getBaseUrl() : "https://api.deepseek.com";
        
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(baseUrl)
            .build();
        
        Double temperature = getDoubleProperty(config, "temperature", 0.7);
        
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(temperature)
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "deepseek";
    }
    
    private Double getDoubleProperty(ModelConfig config, String key, Double defaultValue) {
        Object value = config.getProperties().get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
