package io.mayfly.failover;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 故障转移配置
 */
@Data
@Builder
public class FailoverConfig {
    
    /** 是否启用故障转移 */
    @Builder.Default
    private boolean enabled = true;
    
    /** 最大重试次数 */
    @Builder.Default
    private int maxRetries = 2;
    
    /** 冷却时间 */
    @Builder.Default
    private Duration cooldownDuration = Duration.ofSeconds(60);
    
    /** 可重试的异常类型 */
    @Builder.Default
    private List<String> retryableExceptions = Arrays.asList(
        "java.net.SocketTimeoutException",
        "org.springframework.web.client.HttpServerErrorException",
        "org.springframework.web.client.ResourceAccessException"
    );
}
