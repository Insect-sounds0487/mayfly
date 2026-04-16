package io.mayfly.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelInstance 单元测试
 */
class ModelInstanceTest {
    
    private ModelConfig config;
    
    @BeforeEach
    void setUp() {
        config = ModelConfig.builder()
            .name("test-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .weight(100)
            .enabled(true)
            .build();
    }
    
    @Test
    @DisplayName("测试模型实例创建")
    void testModelInstanceCreation() {
        ModelInstance instance = new ModelInstance(config, null);
        
        assertNotNull(instance);
        assertEquals("test-model", instance.getConfig().getName());
        assertEquals(HealthStatus.HEALTHY, instance.getHealthStatus());
        assertTrue(instance.isAvailable());
    }
    
    @Test
    @DisplayName("测试记录成功请求")
    void testRecordSuccess() {
        ModelInstance instance = new ModelInstance(config, null);
        instance.getActiveRequests().incrementAndGet();
        
        instance.recordSuccess(100);
        
        assertEquals(1, instance.getTotalRequests().get());
        assertEquals(0, instance.getFailedRequests().get());
        assertEquals(0, instance.getActiveRequests().get());
    }
    
    @Test
    @DisplayName("测试记录失败请求")
    void testRecordFailure() {
        ModelInstance instance = new ModelInstance(config, null);
        instance.getActiveRequests().incrementAndGet();
        
        instance.recordFailure();
        
        assertEquals(0, instance.getTotalRequests().get());
        assertEquals(1, instance.getFailedRequests().get());
        assertEquals(0, instance.getActiveRequests().get());
    }
    
    @Test
    @DisplayName("测试活跃请求保护 - 防止负数")
    void testActiveRequestsProtection() {
        ModelInstance instance = new ModelInstance(config, null);
        
        assertEquals(0, instance.getActiveRequests().get());
        
        instance.recordSuccess(100);
        
        assertEquals(0, instance.getActiveRequests().get());
    }
    
    @Test
    @DisplayName("测试模型不可用 - 禁用状态")
    void testModelUnavailableWhenDisabled() {
        ModelConfig disabledConfig = ModelConfig.builder()
            .name("disabled-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .enabled(false)
            .build();
        
        ModelInstance instance = new ModelInstance(disabledConfig, null);
        
        assertFalse(instance.isAvailable());
    }
    
    @Test
    @DisplayName("测试模型不可用 - 不健康状态")
    void testModelUnavailableWhenUnhealthy() {
        ModelInstance instance = new ModelInstance(config, null);
        instance.setHealthStatus(HealthStatus.UNHEALTHY);
        
        assertFalse(instance.isAvailable());
    }
    
    @Test
    @DisplayName("测试模型不可用 - 冷却状态")
    void testModelUnavailableWhenCooldown() {
        ModelInstance instance = new ModelInstance(config, null);
        instance.setHealthStatus(HealthStatus.COOLDOWN);
        instance.setCooldownUntil(java.time.Instant.now().plusSeconds(60));
        
        assertFalse(instance.isAvailable());
    }
    
    @Test
    @DisplayName("测试失败率计算")
    void testFailureRateCalculation() {
        ModelInstance instance = new ModelInstance(config, null);
        
        assertEquals(0.0, instance.getFailureRate());
        
        instance.recordSuccess(100);
        instance.recordSuccess(100);
        instance.recordFailure();
        instance.recordFailure();
        
        assertEquals(0.5, instance.getFailureRate());
    }
}
