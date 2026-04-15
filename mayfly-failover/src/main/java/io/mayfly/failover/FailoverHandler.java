package io.mayfly.failover;

import io.mayfly.core.HealthStatus;
import io.mayfly.core.ModelInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 故障转移处理器
 */
@Component
@Slf4j
public class FailoverHandler {
    
    private final FailoverConfig config;
    
    public FailoverHandler() {
        this.config = FailoverConfig.builder().build();
    }
    
    public FailoverHandler(FailoverConfig config) {
        this.config = config;
    }
    
    /**
     * 执行故障转移
     */
    public FailoverResult executeFailover(
            Prompt request,
            ModelInstance failedModel,
            List<ModelInstance> candidates,
            Exception exception) {
        
        log.warn("Model {} failed, executing failover. Error: {}", 
            failedModel.getConfig().getName(), exception.getMessage());
        
        failedModel.setHealthStatus(HealthStatus.COOLDOWN);
        failedModel.setCooldownUntil(
            Instant.now().plus(config.getCooldownDuration()));
        
        List<ModelInstance> backups = candidates.stream()
            .filter(m -> !m.getConfig().getName().equals(
                failedModel.getConfig().getName()))
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
        
        if (backups.isEmpty()) {
            log.error("No backup models available for failover");
            return FailoverResult.failure("No backup available");
        }
        
        ModelInstance backup = backups.get(0);
        log.info("Failover to model: {}", backup.getConfig().getName());
        
        return FailoverResult.success(backup);
    }
    
    /**
     * 判断异常是否可重试
     */
    public boolean isRetryable(Exception exception) {
        String exceptionName = exception.getClass().getName();
        return config.getRetryableExceptions().contains(exceptionName);
    }
}
