package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * 百度文心一言 模型适配器
 * 支持 ERNIE-4.0, ERNIE-3.5 等模型
 * 
 * 文心一言API兼容OpenAI格式，可通过OpenAI兼容接口调用
 */
@Component
public class WenxinModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "wenxin";
    private static final String DEFAULT_BASE_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Wenxin API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "ernie-4.0-8k";
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
