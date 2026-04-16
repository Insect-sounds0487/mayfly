package io.mayfly.loadbalancer;

import io.mayfly.core.ModelInstance;

import java.util.List;

/**
 * 负载均衡器接口
 */
public interface LoadBalancer {
    
    /**
     * 选择目标模型
     * @param candidates 候选模型列表
     * @return 选中的模型实例
     */
    ModelInstance choose(List<ModelInstance> candidates);
    
    /**
     * 负载均衡器名称
     */
    String getName();
}
