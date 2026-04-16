package io.mayfly.router.impl;

import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.router.RouterStrategy;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 权重路由策略
 * 按权重比例分配请求
 */
@Component
public class WeightedRouterStrategy implements RouterStrategy {
    
    private final Random random = new Random();
    
    @Override
    public ModelInstance select(Prompt request, List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        int totalWeight = available.stream()
            .mapToInt(m -> m.getConfig().getWeight())
            .sum();
        
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (ModelInstance model : available) {
            currentWeight += model.getConfig().getWeight();
            if (randomValue < currentWeight) {
                return model;
            }
        }
        
        return available.get(available.size() - 1);
    }
    
    @Override
    public String getName() {
        return "weighted";
    }
}
