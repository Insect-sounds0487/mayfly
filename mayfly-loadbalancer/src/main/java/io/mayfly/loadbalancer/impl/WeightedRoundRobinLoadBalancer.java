package io.mayfly.loadbalancer.impl;

import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.loadbalancer.LoadBalancer;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 加权轮询负载均衡器
 * 使用平滑加权轮询算法
 */
@Component
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    
    private final Map<String, WeightedNode> nodeMap = new ConcurrentHashMap<>();
    
    @Override
    public synchronized ModelInstance choose(List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        for (ModelInstance instance : available) {
            nodeMap.computeIfAbsent(instance.getConfig().getName(), 
                k -> new WeightedNode(instance));
            WeightedNode node = nodeMap.get(instance.getConfig().getName());
            node.setWeight(instance.getConfig().getWeight());
        }
        
        WeightedNode selected = null;
        int totalWeight = 0;
        
        for (WeightedNode node : nodeMap.values()) {
            if (!node.getInstance().isAvailable()) {
                continue;
            }
            node.setCurrentWeight(node.getCurrentWeight() + node.getWeight());
            totalWeight += node.getWeight();
            
            if (selected == null || 
                node.getCurrentWeight() > selected.getCurrentWeight()) {
                selected = node;
            }
        }
        
        if (selected != null) {
            selected.setCurrentWeight(selected.getCurrentWeight() - totalWeight);
            return selected.getInstance();
        }
        
        return available.get(0);
    }
    
    @Override
    public String getName() {
        return "weighted-round-robin";
    }
    
    @Data
    private static class WeightedNode {
        private final ModelInstance instance;
        private int weight;
        private int currentWeight;
        
        public WeightedNode(ModelInstance instance) {
            this.instance = instance;
            this.weight = instance.getConfig().getWeight();
            this.currentWeight = 0;
        }
    }
}
