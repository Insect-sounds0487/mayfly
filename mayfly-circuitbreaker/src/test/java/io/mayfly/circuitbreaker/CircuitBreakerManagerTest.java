package io.mayfly.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircuitBreakerManager 单元测试
 */
class CircuitBreakerManagerTest {
    
    private CircuitBreakerManager manager;
    
    @BeforeEach
    void setUp() {
        CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ZERO)
            .build();
        
        manager = new CircuitBreakerManager(config);
    }
    
    @Test
    void testGetCircuitBreaker() {
        CircuitBreaker cb1 = manager.getCircuitBreaker("model-1");
        CircuitBreaker cb2 = manager.getCircuitBreaker("model-1");
        CircuitBreaker cb3 = manager.getCircuitBreaker("model-2");
        
        assertNotNull(cb1);
        assertSame(cb1, cb2);
        assertNotSame(cb1, cb3);
        assertEquals("model-1", cb1.getName());
        assertEquals("model-2", cb3.getName());
    }
    
    @Test
    void testGetRateLimiter() {
        RateLimiter rl1 = manager.getRateLimiter("model-1");
        RateLimiter rl2 = manager.getRateLimiter("model-1");
        RateLimiter rl3 = manager.getRateLimiter("model-2");
        
        assertNotNull(rl1);
        assertSame(rl1, rl2);
        assertNotSame(rl1, rl3);
    }
    
    @Test
    void testExecuteProtected_Success() {
        String result = manager.executeProtected("model-1", () -> "success");
        
        assertEquals("success", result);
        
        CircuitBreaker cb = manager.getCircuitBreaker("model-1");
        assertEquals(State.CLOSED, cb.getState());
    }
    
    @Test
    void testExecuteProtected_Exception() {
        assertThrows(RuntimeException.class, () -> {
            manager.executeProtected("model-1", () -> {
                throw new RuntimeException("Test exception");
            });
        });
    }
    
    @Test
    void testCircuitBreaker_TransitionToOpen() {
        String modelName = "test-model";
        CircuitBreaker cb = manager.getCircuitBreaker(modelName);
        
        assertEquals(State.CLOSED, cb.getState());
        
        for (int i = 0; i < 10; i++) {
            try {
                manager.executeProtected(modelName, () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
            }
        }
        
        State state = cb.getState();
        assertTrue(state == State.OPEN || state == State.HALF_OPEN, 
            "Circuit breaker should be OPEN or HALF_OPEN after multiple failures");
    }
    
    @Test
    void testRateLimiter_AllowRequests() {
        String modelName = "rate-limited-model";
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            String result = manager.executeProtected(modelName, () -> "ok-" + index);
            assertEquals("ok-" + index, result);
        }
    }
    
    @Test
    void testDefaultConstructor() {
        CircuitBreakerManager defaultManager = new CircuitBreakerManager();
        
        assertNotNull(defaultManager);
        
        CircuitBreaker cb = defaultManager.getCircuitBreaker("default-model");
        assertNotNull(cb);
        
        String result = defaultManager.executeProtected("default-model", () -> "test");
        assertEquals("test", result);
    }
    
    @Test
    void testCircuitBreakerConfigProperties_DefaultValues() {
        CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder().build();
        
        assertEquals(50, config.getFailureRateThreshold());
        assertEquals(Duration.ofSeconds(60), config.getWaitDurationInOpenState());
        assertEquals(10, config.getSlidingWindowSize());
        assertEquals(5, config.getMinimumNumberOfCalls());
        assertEquals(Duration.ofSeconds(1), config.getLimitRefreshPeriod());
        assertEquals(100, config.getLimitForPeriod());
        assertEquals(Duration.ZERO, config.getTimeoutDuration());
    }
    
    @Test
    void testCircuitBreakerConfigProperties_CustomValues() {
        CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
            .failureRateThreshold(30)
            .waitDurationInOpenState(Duration.ofSeconds(120))
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .limitRefreshPeriod(Duration.ofSeconds(2))
            .limitForPeriod(50)
            .timeoutDuration(Duration.ofMillis(500))
            .build();
        
        assertEquals(30, config.getFailureRateThreshold());
        assertEquals(Duration.ofSeconds(120), config.getWaitDurationInOpenState());
        assertEquals(20, config.getSlidingWindowSize());
        assertEquals(10, config.getMinimumNumberOfCalls());
        assertEquals(Duration.ofSeconds(2), config.getLimitRefreshPeriod());
        assertEquals(50, config.getLimitForPeriod());
        assertEquals(Duration.ofMillis(500), config.getTimeoutDuration());
    }
    
    @Test
    void testExecuteProtected_WithComplexObject() {
        TestResult expected = new TestResult("data", 42);
        
        TestResult actual = manager.executeProtected("model-complex", () -> expected);
        
        assertNotNull(actual);
        assertEquals("data", actual.getData());
        assertEquals(42, actual.getCount());
    }
    
    @Test
    void testMultipleModels_Isolation() {
        manager.executeProtected("model-a", () -> "A");
        manager.executeProtected("model-b", () -> "B");
        
        CircuitBreaker cbA = manager.getCircuitBreaker("model-a");
        CircuitBreaker cbB = manager.getCircuitBreaker("model-b");
        
        assertNotSame(cbA, cbB);
        assertEquals(State.CLOSED, cbA.getState());
        assertEquals(State.CLOSED, cbB.getState());
    }
    
    static class TestResult {
        private final String data;
        private final int count;
        
        TestResult(String data, int count) {
            this.data = data;
            this.count = count;
        }
        
        String getData() { return data; }
        int getCount() { return count; }
    }
}
