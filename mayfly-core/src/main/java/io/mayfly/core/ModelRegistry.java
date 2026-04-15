package io.mayfly.core;

import java.util.List;
import java.util.Optional;

/**
 * 模型注册中心
 * 管理所有可用模型实例
 */
public interface ModelRegistry {
    
    /**
     * 注册模型
     * @param config 模型配置
     */
    void register(ModelConfig config);
    
    /**
     * 获取模型实例
     * @param name 模型名称
     * @return 模型实例
     */
    Optional<ModelInstance> getModel(String name);
    
    /**
     * 获取所有可用模型
     * @return 可用模型列表
     */
    List<ModelInstance> getAllAvailableModels();
    
    /**
     * 获取所有模型（包括不可用）
     * @return 所有模型列表
     */
    List<ModelInstance> getAllModels();
    
    /**
     * 更新模型配置
     * @param name 模型名称
     * @param config 新配置
     */
    void updateModel(String name, ModelConfig config);
    
    /**
     * 移除模型
     * @param name 模型名称
     */
    void removeModel(String name);
    
    /**
     * 根据标签获取模型
     * @param tag 标签
     * @return 匹配的模型列表
     */
    List<ModelInstance> getModelsByTag(String tag);
}
