package io.mayfly.core;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型配置
 */
@Data
@Builder
public class ModelConfig {
    
    /** 模型唯一标识 */
    private String name;
    
    /** 模型提供商 (zhipu, tongyi, deepseek等) */
    private String provider;
    
    /** 模型ID (glm-4, qwen-max等) */
    private String model;
    
    /** API Key */
    private String apiKey;
    
    /** API Base URL (可选，用于私有化部署) */
    private String baseUrl;
    
    /** 权重 (用于负载均衡) */
    @Builder.Default
    private int weight = 100;
    
    /** 是否启用 */
    @Builder.Default
    private boolean enabled = true;
    
    /** 超时时间(毫秒) */
    @Builder.Default
    private long timeout = 30000;
    
    /** 最大重试次数 */
    @Builder.Default
    private int maxRetries = 2;
    
    /** 模型标签 (用于分组) */
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    /** 额外参数 */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}
