package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.dashscope.DashScopeChatModel;
import org.springframework.ai.dashscope.DashScopeChatOptions;
import org.springframework.ai.dashscope.api.DashScopeApi;
import org.springframework.stereotype.Component;

/**
 * 通义千问适配器
 */
@Component
public class TongyiModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API key must not be empty for provider: tongyi");
        }
        
        DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        
        Double temperature = getDoubleProperty(config, "temperature", 0.7);
        
        return DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi)
            .defaultOptions(DashScopeChatOptions.builder()
                .model(config.getModel())
                .temperature(temperature)
                .build())
            .build();
    }
    
    @Override
    public String getProvider() {
        return "tongyi";
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
