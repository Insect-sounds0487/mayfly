package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.stereotype.Component;

/**
 * Anthropic Claude 模型适配器
 * 支持 Claude-3.5-Sonnet, Claude-3-Opus 等模型
 */
@Component
public class ClaudeModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "claude";
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Claude API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "claude-3-5-sonnet-20241022";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank() 
            ? config.getBaseUrl() 
            : DEFAULT_BASE_URL;
        
        AnthropicApi anthropicApi = new AnthropicApi(config.getApiKey(), baseUrl);
        
        AnthropicChatOptions options = new AnthropicChatOptions();
        options.setModel(model);
        options.setTemperature(0.7);
        options.setMaxTokens(4096);
        
        return new AnthropicChatModel(anthropicApi, options);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER;
    }
}
