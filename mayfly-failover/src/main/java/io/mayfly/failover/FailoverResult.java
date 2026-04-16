package io.mayfly.failover;

import io.mayfly.core.ModelInstance;
import lombok.Data;

/**
 * 故障转移结果
 */
@Data
public class FailoverResult {
    
    private final boolean success;
    private final ModelInstance targetModel;
    private final String errorMessage;
    
    private FailoverResult(boolean success, ModelInstance targetModel, String errorMessage) {
        this.success = success;
        this.targetModel = targetModel;
        this.errorMessage = errorMessage;
    }
    
    public static FailoverResult success(ModelInstance targetModel) {
        return new FailoverResult(true, targetModel, null);
    }
    
    public static FailoverResult failure(String errorMessage) {
        return new FailoverResult(false, null, errorMessage);
    }
}
