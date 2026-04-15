package io.mayfly.loadbalancer.impl;

import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.loadbalancer.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 轮询负载均衡器
 */
@Component
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ModelInstance choose(List<ModelInstance> candidates) {
        List<ModelInstance> available = candidates.stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (available.isEmpty()) {
            throw new ModelUnavailableException("No available model");
        }
        
        int index = Math.abs(counter.getAndIncrement() % available.size());
        return available.get(index);
    }
    
    @Override
    public String getName() {
        return "round-robin";
    }
}
