package io.mayfly.router;

import lombok.Builder;
import lombok.Data;

/**
 * 路由规则
 */
@Data
@Builder
public class RouterRule {
    
    /** 规则名称 */
    private String name;
    
    /** SpEL条件表达式 */
    private String condition;
    
    /** 目标模型名称 */
    private String targetModel;
    
    /** 优先级 (数字越小优先级越高) */
    @Builder.Default
    private int priority = 100;
}
