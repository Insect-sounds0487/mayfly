package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.zhipuai.ZhipuAiChatModel;
import org.springframework.ai.zhipuai.ZhipuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhipuAiApi;
import org.springframework.stereotype.Component;

/**
 * 智谱AI适配器
 */
@Component
public class ZhipuModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API key must not be empty for provider: zhipu");
        }
        
        ZhipuAiApi zhipuAiApi = new ZhipuAiApi.Builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        
        Double temperature = getDoubleProperty(config, "temperature", 0.7);
        Double topP = getDoubleProperty(config, "top_p", 0.9);
        
        return ZhipuAiChatModel.builder()
            .zhipuAiApi(zhipuAiApi)
            .defaultOptions(ZhipuAiChatOptions.builder()
                .model(config.getModel())
                .temperature(temperature)
                .topP(topP)
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "zhipu";
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getProperty(ModelConfig config, String key, T defaultValue) {
        Object value = config.getProperties().get(key);
        return value != null ? (T) value : defaultValue;
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
