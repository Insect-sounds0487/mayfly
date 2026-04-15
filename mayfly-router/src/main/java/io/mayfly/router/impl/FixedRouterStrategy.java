package io.mayfly.router.impl;

import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.router.RouterStrategy;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 固定路由策略
 * 始终路由到第一个可用的模型
 */
@Component
public class FixedRouterStrategy implements RouterStrategy {
    
    @Override
    public ModelInstance select(Prompt request, List<ModelInstance> candidates) {
        return candidates.stream()
            .filter(ModelInstance::isAvailable)
            .findFirst()
            .orElseThrow(() -> new ModelUnavailableException("No available model"));
    }
    
    @Override
    public String getName() {
        return "fixed";
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
}
