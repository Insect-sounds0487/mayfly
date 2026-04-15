package io.mayfly.router;

import io.mayfly.core.ModelInstance;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 路由策略接口
 */
public interface RouterStrategy extends Ordered {
    
    /**
     * 从候选模型中选择一个
     * @param prompt 聊天提示
     * @param candidates 候选模型列表
     * @return 选中的模型
     */
    ModelInstance select(Prompt prompt, List<ModelInstance> candidates);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getName();
    
    @Override
    default int getOrder() {
        return 0;
    }
}
