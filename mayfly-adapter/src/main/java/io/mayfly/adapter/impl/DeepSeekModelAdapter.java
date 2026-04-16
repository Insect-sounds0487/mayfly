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
 * DeepSeek 兼容 OpenAI API
 */
@Component
public class DeepSeekModelAdapter implements ModelAdapter {
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        OpenAiApi openAiApi = new OpenAiApi(
            config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.deepseek.com",
            config.getApiKey()
        );
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(config.getModel())
            .temperature(0.7)
            .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }
    
    @Override
    public String getProvider() {
        return "deepseek";
    }
}
