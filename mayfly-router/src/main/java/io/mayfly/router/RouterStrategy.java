package io.mayfly.router;

import io.mayfly.core.ModelInstance;
import org.springframework.ai.chat.prompt.ChatRequest;

/**
 * 路由策略接口
 */
public interface RouterStrategy {
    
    /**
     * 选择目标模型
     * @param request 聊天请求
     * @param candidates 候选模型列表
     * @return 选中的模型实例
     */
    ModelInstance select(ChatRequest request, java.util.List<ModelInstance> candidates);
    
    /**
     * 策略名称
     */
    String getName();
    
    /**
     * 策略优先级 (数字越小优先级越高)
     */
    default int getOrder() {
        return 100;
    }
}
