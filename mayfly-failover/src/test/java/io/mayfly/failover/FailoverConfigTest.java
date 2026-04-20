package io.mayfly.failover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FailoverConfig 单元测试")
class FailoverConfigTest {
    
    @Test
    @DisplayName("测试使用默认值创建配置")
    void testDefaultConfig() {
        FailoverConfig config = FailoverConfig.builder().build();
        
        assertTrue(config.isEnabled());
        assertEquals(2, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(60), config.getCooldownDuration());
        assertNotNull(config.getRetryableExceptions());
        assertEquals(3, config.getRetryableExceptions().size());
    }
    
    @Test
    @DisplayName("测试使用自定义值创建配置")
    void testCustomConfig() {
        List<String> customExceptions = Arrays.asList(
            "java.io.IOException",
            "java.net.ConnectException"
        );
        
        FailoverConfig config = FailoverConfig.builder()
            .enabled(false)
            .maxRetries(5)
            .cooldownDuration(Duration.ofMinutes(5))
            .retryableExceptions(customExceptions)
            .build();
        
        assertFalse(config.isEnabled());
        assertEquals(5, config.getMaxRetries());
        assertEquals(Duration.ofMinutes(5), config.getCooldownDuration());
        assertEquals(2, config.getRetryableExceptions().size());
        assertTrue(config.getRetryableExceptions().contains("java.io.IOException"));
        assertTrue(config.getRetryableExceptions().contains("java.net.ConnectException"));
    }
    
    @Test
    @DisplayName("测试禁用故障转移")
    void testDisabledFailover() {
        FailoverConfig config = FailoverConfig.builder()
            .enabled(false)
            .build();
        
        assertFalse(config.isEnabled());
    }
    
    @Test
    @DisplayName("测试最大重试次数边界值")
    void testMaxRetriesBoundary() {
        FailoverConfig zeroRetries = FailoverConfig.builder()
            .maxRetries(0)
            .build();
        
        assertEquals(0, zeroRetries.getMaxRetries());
        
        FailoverConfig highRetries = FailoverConfig.builder()
            .maxRetries(10)
            .build();
        
        assertEquals(10, highRetries.getMaxRetries());
    }
    
    @Test
    @DisplayName("测试冷却时间配置")
    void testCooldownDuration() {
        FailoverConfig config = FailoverConfig.builder()
            .cooldownDuration(Duration.ofSeconds(30))
            .build();
        
        assertEquals(Duration.ofSeconds(30), config.getCooldownDuration());
        
        FailoverConfig longCooldown = FailoverConfig.builder()
            .cooldownDuration(Duration.ofHours(1))
            .build();
        
        assertEquals(Duration.ofHours(1), longCooldown.getCooldownDuration());
    }
    
    @Test
    @DisplayName("测试可重试异常列表")
    void testRetryableExceptions() {
        FailoverConfig config = FailoverConfig.builder().build();
        
        List<String> exceptions = config.getRetryableExceptions();
        
        assertTrue(exceptions.contains("java.net.SocketTimeoutException"));
        assertTrue(exceptions.contains("org.springframework.web.client.HttpServerErrorException"));
        assertTrue(exceptions.contains("org.springframework.web.client.ResourceAccessException"));
    }
    
    @Test
    @DisplayName("测试自定义可重试异常列表")
    void testCustomRetryableExceptions() {
        List<String> customExceptions = Arrays.asList(
            "com.example.CustomException",
            "com.example.AnotherException"
        );
        
        FailoverConfig config = FailoverConfig.builder()
            .retryableExceptions(customExceptions)
            .build();
        
        assertEquals(2, config.getRetryableExceptions().size());
        assertTrue(config.getRetryableExceptions().contains("com.example.CustomException"));
        assertTrue(config.getRetryableExceptions().contains("com.example.AnotherException"));
    }
    
    @Test
    @DisplayName("测试Getter和Setter")
    void testGettersAndSetters() {
        FailoverConfig config = FailoverConfig.builder().build();
        
        config.setEnabled(false);
        config.setMaxRetries(3);
        config.setCooldownDuration(Duration.ofSeconds(120));
        config.setRetryableExceptions(Arrays.asList("java.lang.Exception"));
        
        assertFalse(config.isEnabled());
        assertEquals(3, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(120), config.getCooldownDuration());
        assertEquals(1, config.getRetryableExceptions().size());
    }
}
