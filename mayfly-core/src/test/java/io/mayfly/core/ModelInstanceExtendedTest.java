package io.mayfly.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelInstance 扩展测试")
class ModelInstanceExtendedTest {
    
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
    
    @Nested
    @DisplayName("可用性测试")
    class AvailabilityTests {
        
        @Test
        @DisplayName("测试冷却状态但冷却已结束 - 应该可用")
        void testCooldownExpired_ShouldBeAvailable() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.COOLDOWN);
            instance.setCooldownUntil(Instant.now().minusSeconds(10));
            
            assertTrue(instance.isAvailable());
        }
        
        @Test
        @DisplayName("测试冷却状态且冷却未结束 - 应该不可用")
        void testCooldownActive_ShouldBeUnavailable() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.COOLDOWN);
            instance.setCooldownUntil(Instant.now().plusSeconds(60));
            
            assertFalse(instance.isAvailable());
        }
        
        @Test
        @DisplayName("测试健康状态为HEALTHY - 应该可用")
        void testHealthyStatus_ShouldBeAvailable() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.HEALTHY);
            
            assertTrue(instance.isAvailable());
        }
    }
    
    @Nested
    @DisplayName("失败率计算测试")
    class FailureRateTests {
        
        @Test
        @DisplayName("测试无请求时失败率为0")
        void testNoRequests_FailureRateZero() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertEquals(0.0, instance.getFailureRate());
        }
        
        @Test
        @DisplayName("测试全部成功时失败率为0")
        void testAllSuccess_FailureRateZero() {
            ModelInstance instance = new ModelInstance(config, null);
            
            for (int i = 0; i < 10; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordSuccess(100);
            }
            
            assertEquals(0.0, instance.getFailureRate());
            assertEquals(10, instance.getTotalRequests().get());
            assertEquals(0, instance.getFailedRequests().get());
        }
        
        @Test
        @DisplayName("测试全部失败时失败率为1.0")
        void testAllFailure_FailureRateOne() {
            ModelInstance instance = new ModelInstance(config, null);
            
            for (int i = 0; i < 10; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordFailure();
            }
            
            assertEquals(1.0, instance.getFailureRate());
            assertEquals(10, instance.getTotalRequests().get());
            assertEquals(10, instance.getFailedRequests().get());
        }
        
        @Test
        @DisplayName("测试50%失败率")
        void testHalfFailure_FailureRateHalf() {
            ModelInstance instance = new ModelInstance(config, null);
            
            for (int i = 0; i < 5; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordSuccess(100);
            }
            
            for (int i = 0; i < 5; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordFailure();
            }
            
            assertEquals(0.5, instance.getFailureRate());
            assertEquals(10, instance.getTotalRequests().get());
            assertEquals(5, instance.getFailedRequests().get());
        }
        
        @Test
        @DisplayName("测试混合请求的失败率计算")
        void testMixedRequests_FailureRateCalculation() {
            ModelInstance instance = new ModelInstance(config, null);
            
            for (int i = 0; i < 7; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordSuccess(100);
            }
            
            for (int i = 0; i < 3; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordFailure();
            }
            
            assertEquals(0.3, instance.getFailureRate());
            assertEquals(10, instance.getTotalRequests().get());
            assertEquals(3, instance.getFailedRequests().get());
        }
    }
    
    @Nested
    @DisplayName("延迟计算测试")
    class LatencyTests {
        
        @Test
        @DisplayName("测试首次成功后的延迟记录")
        void testFirstSuccess_LatencyRecorded() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.getActiveRequests().incrementAndGet();
            instance.recordSuccess(500);
            
            assertEquals(50, instance.getAvgLatency().get());
        }
        
        @Test
        @DisplayName("测试多次成功后的加权平均延迟")
        void testMultipleSuccess_WeightedAverageLatency() {
            ModelInstance instance = new ModelInstance(config, null);
            
            instance.getActiveRequests().incrementAndGet();
            instance.recordSuccess(1000);
            
            instance.getActiveRequests().incrementAndGet();
            instance.recordSuccess(200);
            
            long firstAvg = (long) (0 * 0.9 + 1000 * 0.1);
            long expectedAvg = (long) (firstAvg * 0.9 + 200 * 0.1);
            assertEquals(expectedAvg, instance.getAvgLatency().get());
        }
        
        @Test
        @DisplayName("测试延迟衰减机制")
        void testLatencyDecay() {
            ModelInstance instance = new ModelInstance(config, null);
            
            instance.getActiveRequests().incrementAndGet();
            instance.recordSuccess(1000);
            
            for (int i = 0; i < 10; i++) {
                instance.getActiveRequests().incrementAndGet();
                instance.recordSuccess(100);
            }
            
            long finalAvg = instance.getAvgLatency().get();
            assertTrue(finalAvg < 1000);
            assertTrue(finalAvg > 0);
        }
    }
    
    @Nested
    @DisplayName("活跃请求管理测试")
    class ActiveRequestTests {
        
        @Test
        @DisplayName("测试活跃请求递增")
        void testActiveRequestsIncrement() {
            ModelInstance instance = new ModelInstance(config, null);
            
            instance.getActiveRequests().incrementAndGet();
            instance.getActiveRequests().incrementAndGet();
            instance.getActiveRequests().incrementAndGet();
            
            assertEquals(3, instance.getActiveRequests().get());
        }
        
        @Test
        @DisplayName("测试记录成功后活跃请求递减")
        void testRecordSuccess_DecrementsActive() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.getActiveRequests().set(5);
            
            instance.recordSuccess(100);
            
            assertEquals(4, instance.getActiveRequests().get());
        }
        
        @Test
        @DisplayName("测试记录失败后活跃请求递减")
        void testRecordFailure_DecrementsActive() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.getActiveRequests().set(5);
            
            instance.recordFailure();
            
            assertEquals(4, instance.getActiveRequests().get());
        }
        
        @Test
        @DisplayName("测试活跃请求为0时记录成功 - 防止负数")
        void testRecordSuccess_ZeroActive_PreventsNegative() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertEquals(0, instance.getActiveRequests().get());
            
            instance.recordSuccess(100);
            
            assertEquals(0, instance.getActiveRequests().get());
        }
        
        @Test
        @DisplayName("测试活跃请求为0时记录失败 - 防止负数")
        void testRecordFailure_ZeroActive_PreventsNegative() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertEquals(0, instance.getActiveRequests().get());
            
            instance.recordFailure();
            
            assertEquals(0, instance.getActiveRequests().get());
        }
    }
    
    @Nested
    @DisplayName("健康状态管理测试")
    class HealthStatusTests {
        
        @Test
        @DisplayName("测试初始健康状态为HEALTHY")
        void testInitialHealthStatus() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertEquals(HealthStatus.HEALTHY, instance.getHealthStatus());
        }
        
        @Test
        @DisplayName("测试设置健康状态为UNHEALTHY")
        void testSetUnhealthyStatus() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.UNHEALTHY);
            
            assertEquals(HealthStatus.UNHEALTHY, instance.getHealthStatus());
        }
        
        @Test
        @DisplayName("测试设置健康状态为COOLDOWN")
        void testSetCooldownStatus() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.COOLDOWN);
            
            assertEquals(HealthStatus.COOLDOWN, instance.getHealthStatus());
        }
        
        @Test
        @DisplayName("测试从UNHEALTHY恢复到HEALTHY")
        void testRecoverFromUnhealthy() {
            ModelInstance instance = new ModelInstance(config, null);
            instance.setHealthStatus(HealthStatus.UNHEALTHY);
            assertFalse(instance.isAvailable());
            
            instance.setHealthStatus(HealthStatus.HEALTHY);
            assertTrue(instance.isAvailable());
        }
    }
    
    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTests {
        
        @Test
        @DisplayName("测试初始统计值")
        void testInitialStatistics() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertEquals(0, instance.getTotalRequests().get());
            assertEquals(0, instance.getFailedRequests().get());
            assertEquals(0, instance.getActiveRequests().get());
            assertEquals(0, instance.getAvgLatency().get());
        }
        
        @Test
        @DisplayName("测试大量请求后的统计信息")
        void testStatisticsAfterManyRequests() {
            ModelInstance instance = new ModelInstance(config, null);
            
            for (int i = 0; i < 100; i++) {
                instance.getActiveRequests().incrementAndGet();
                if (i % 4 == 0) {
                    instance.recordFailure();
                } else {
                    instance.recordSuccess(150);
                }
            }
            
            assertEquals(100, instance.getTotalRequests().get());
            assertEquals(25, instance.getFailedRequests().get());
            assertEquals(0, instance.getActiveRequests().get());
            assertEquals(0.25, instance.getFailureRate());
        }
    }
    
    @Nested
    @DisplayName("配置引用测试")
    class ConfigReferenceTests {
        
        @Test
        @DisplayName("测试模型实例正确引用配置")
        void testInstanceReferencesConfig() {
            ModelInstance instance = new ModelInstance(config, null);
            
            assertSame(config, instance.getConfig());
            assertEquals("test-model", instance.getConfig().getName());
            assertEquals("test", instance.getConfig().getProvider());
        }
        
        @Test
        @DisplayName("测试配置更新影响实例可用性")
        void testConfigUpdateAffectsAvailability() {
            ModelInstance instance = new ModelInstance(config, null);
            assertTrue(instance.isAvailable());
            
            config.setEnabled(false);
            assertFalse(instance.isAvailable());
        }
    }
}
