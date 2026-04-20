package io.mayfly.circuitbreaker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CircuitBreakerConfigProperties 单元测试")
class CircuitBreakerConfigPropertiesTest {
    
    @Test
    @DisplayName("测试使用默认值创建配置")
    void testDefaultConfig() {
        CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder().build();
        
        assertEquals(50, config.getFailureRateThreshold());
        assertEquals(Duration.ofSeconds(60), config.getWaitDurationInOpenState());
        assertEquals(10, config.getSlidingWindowSize());
        assertEquals(5, config.getMinimumNumberOfCalls());
        assertEquals(Duration.ofSeconds(1), config.getLimitRefreshPeriod());
        assertEquals(100, config.getLimitForPeriod());
        assertEquals(Duration.ZERO, config.getTimeoutDuration());
    }
    
    @Nested
    @DisplayName("熔断器配置测试")
    class CircuitBreakerConfigTests {
        
        @Test
        @DisplayName("测试自定义失败率阈值")
        void testCustomFailureRateThreshold() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(30)
                .build();
            
            assertEquals(30, config.getFailureRateThreshold());
        }
        
        @Test
        @DisplayName("测试失败率阈值边界值 - 0")
        void testFailureRateThresholdZero() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(0)
                .build();
            
            assertEquals(0, config.getFailureRateThreshold());
        }
        
        @Test
        @DisplayName("测试失败率阈值边界值 - 100")
        void testFailureRateThresholdHundred() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(100)
                .build();
            
            assertEquals(100, config.getFailureRateThreshold());
        }
        
        @Test
        @DisplayName("测试自定义等待时间")
        void testCustomWaitDuration() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .build();
            
            assertEquals(Duration.ofMinutes(2), config.getWaitDurationInOpenState());
        }
        
        @Test
        @DisplayName("测试自定义滑动窗口大小")
        void testCustomSlidingWindowSize() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .slidingWindowSize(20)
                .build();
            
            assertEquals(20, config.getSlidingWindowSize());
        }
        
        @Test
        @DisplayName("测试自定义最小调用次数")
        void testCustomMinimumNumberOfCalls() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .minimumNumberOfCalls(10)
                .build();
            
            assertEquals(10, config.getMinimumNumberOfCalls());
        }
    }
    
    @Nested
    @DisplayName("限流器配置测试")
    class RateLimiterConfigTests {
        
        @Test
        @DisplayName("测试自定义限流周期")
        void testCustomLimitRefreshPeriod() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .build();
            
            assertEquals(Duration.ofSeconds(5), config.getLimitRefreshPeriod());
        }
        
        @Test
        @DisplayName("测试自定义限流周期内允许次数")
        void testCustomLimitForPeriod() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .limitForPeriod(50)
                .build();
            
            assertEquals(50, config.getLimitForPeriod());
        }
        
        @Test
        @DisplayName("测试自定义限流超时时间")
        void testCustomTimeoutDuration() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .timeoutDuration(Duration.ofMillis(500))
                .build();
            
            assertEquals(Duration.ofMillis(500), config.getTimeoutDuration());
        }
        
        @Test
        @DisplayName("测试限流超时时间为零")
        void testTimeoutDurationZero() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .timeoutDuration(Duration.ZERO)
                .build();
            
            assertEquals(Duration.ZERO, config.getTimeoutDuration());
        }
    }
    
    @Nested
    @DisplayName("完整配置测试")
    class FullConfigTests {
        
        @Test
        @DisplayName("测试宽松配置")
        void testLenientConfig() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(80)
                .waitDurationInOpenState(Duration.ofMinutes(5))
                .slidingWindowSize(50)
                .minimumNumberOfCalls(20)
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .limitForPeriod(500)
                .timeoutDuration(Duration.ofSeconds(2))
                .build();
            
            assertEquals(80, config.getFailureRateThreshold());
            assertEquals(Duration.ofMinutes(5), config.getWaitDurationInOpenState());
            assertEquals(50, config.getSlidingWindowSize());
            assertEquals(20, config.getMinimumNumberOfCalls());
            assertEquals(Duration.ofSeconds(10), config.getLimitRefreshPeriod());
            assertEquals(500, config.getLimitForPeriod());
            assertEquals(Duration.ofSeconds(2), config.getTimeoutDuration());
        }
        
        @Test
        @DisplayName("测试严格配置")
        void testStrictConfig() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(20)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(5)
                .minimumNumberOfCalls(2)
                .limitRefreshPeriod(Duration.ofMillis(100))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(100))
                .build();
            
            assertEquals(20, config.getFailureRateThreshold());
            assertEquals(Duration.ofSeconds(10), config.getWaitDurationInOpenState());
            assertEquals(5, config.getSlidingWindowSize());
            assertEquals(2, config.getMinimumNumberOfCalls());
            assertEquals(Duration.ofMillis(100), config.getLimitRefreshPeriod());
            assertEquals(10, config.getLimitForPeriod());
            assertEquals(Duration.ofMillis(100), config.getTimeoutDuration());
        }
    }
    
    @Nested
    @DisplayName("Getter和Setter测试")
    class GetterSetterTests {
        
        @Test
        @DisplayName("测试Setter和Getter")
        void testSettersAndGetters() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder().build();
            
            config.setFailureRateThreshold(75);
            config.setWaitDurationInOpenState(Duration.ofMinutes(3));
            config.setSlidingWindowSize(15);
            config.setMinimumNumberOfCalls(8);
            config.setLimitRefreshPeriod(Duration.ofSeconds(2));
            config.setLimitForPeriod(200);
            config.setTimeoutDuration(Duration.ofSeconds(1));
            
            assertEquals(75, config.getFailureRateThreshold());
            assertEquals(Duration.ofMinutes(3), config.getWaitDurationInOpenState());
            assertEquals(15, config.getSlidingWindowSize());
            assertEquals(8, config.getMinimumNumberOfCalls());
            assertEquals(Duration.ofSeconds(2), config.getLimitRefreshPeriod());
            assertEquals(200, config.getLimitForPeriod());
            assertEquals(Duration.ofSeconds(1), config.getTimeoutDuration());
        }
    }
}
