package io.mayfly.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NoOpMetricsCollector 单元测试
 */
@DisplayName("NoOp监控指标收集器测试")
class NoOpMetricsCollectorTest {
    
    private NoOpMetricsCollector collector;
    
    @BeforeEach
    void setUp() {
        collector = new NoOpMetricsCollector();
    }
    
    @Nested
    @DisplayName("空操作行为测试")
    class NoOpBehaviorTests {
        
        @Test
        @DisplayName("记录成功调用 - 无异常")
        void testRecordSuccess_NoException() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess("test-model", 1000L, 100, 200)
            );
        }
        
        @Test
        @DisplayName("记录失败调用 - 无异常")
        void testRecordFailure_NoException() {
            assertDoesNotThrow(() -> 
                collector.recordFailure("test-model", "TimeoutException")
            );
        }
        
        @Test
        @DisplayName("记录故障转移 - 无异常")
        void testRecordFailover_NoException() {
            assertDoesNotThrow(() -> 
                collector.recordFailover("failed-model", "backup-model")
            );
        }
        
        @Test
        @DisplayName("混合调用 - 全部无异常")
        void testMixedCalls_AllNoException() {
            assertDoesNotThrow(() -> {
                collector.recordSuccess("model-1", 500L, 50, 100);
                collector.recordFailure("model-1", "Error");
                collector.recordFailover("model-1", "model-2");
                collector.recordSuccess("model-2", 800L, 60, 120);
            });
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {
        
        @Test
        @DisplayName("空模型名称")
        void testEmptyModelName() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess("", 0L, 0, 0)
            );
        }
        
        @Test
        @DisplayName("null模型名称")
        void testNullModelName() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess(null, 0L, 0, 0)
            );
        }
        
        @Test
        @DisplayName("零延迟和零Token")
        void testZeroValues() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess("model", 0L, 0, 0)
            );
        }
        
        @Test
        @DisplayName("负数延迟")
        void testNegativeLatency() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess("model", -1L, 0, 0)
            );
        }
        
        @Test
        @DisplayName("极大数值")
        void testLargeValues() {
            assertDoesNotThrow(() -> 
                collector.recordSuccess("model", Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
            );
        }
    }
    
    @Nested
    @DisplayName("多次调用测试")
    class MultipleCallsTests {
        
        @Test
        @DisplayName("重复调用1000次 - 无异常")
        void testRepeatedCalls_1000Times() {
            for (int i = 0; i < 1000; i++) {
                collector.recordSuccess("model", 100L, 10, 20);
            }
            
            assertDoesNotThrow(() -> 
                collector.recordFailure("model", "error")
            );
        }
    }
}
