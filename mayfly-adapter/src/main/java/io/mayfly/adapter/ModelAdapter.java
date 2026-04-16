package io.mayfly.adapter;

import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 模型适配器接口
 */
public interface ModelAdapter {
    
    /**
     * 创建ChatModel实例
     * @param config 模型配置
     * @return ChatModel实例
     */
    ChatModel createChatModel(ModelConfig config);
    
    /**
     * 支持的提供商
     */
    String getProvider();
}
