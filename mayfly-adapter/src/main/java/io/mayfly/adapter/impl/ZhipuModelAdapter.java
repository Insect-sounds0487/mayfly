package io.mayfly.adapter.impl;

import io.mayfly.adapter.ModelAdapter;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

/**
 * 智谱AI 模型适配器
 * 支持 GLM-4, GLM-4-Plus, GLM-3-Turbo 等模型
 */
@Component
public class ZhipuModelAdapter implements ModelAdapter {
    
    private static final String PROVIDER = "zhipu";
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    
    @Override
    public ChatModel createChatModel(ModelConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("Zhipu API Key cannot be null or empty");
        }
        
        String model = config.getModel() != null ? config.getModel() : "glm-4";
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank() 
            ? config.getBaseUrl() 
            : DEFAULT_BASE_URL;
        
        ZhiPuAiApi zhiPuAiApi = new ZhiPuAiApi(config.getApiKey(), baseUrl);
        
        ZhiPuAiChatOptions options = new ZhiPuAiChatOptions();
        options.setModel(model);
        options.setTemperature(0.7);
        
        return new ZhiPuAiChatModel(zhiPuAiApi, options);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER;
    }
}
