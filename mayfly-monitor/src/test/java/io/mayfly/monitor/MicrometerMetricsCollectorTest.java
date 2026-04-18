package io.mayfly.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MicrometerMetricsCollector 单元测试
 */
@DisplayName("Micrometer监控指标收集器测试")
class MicrometerMetricsCollectorTest {
    
    private MeterRegistry meterRegistry;
    private MicrometerMetricsCollector collector;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        collector = new MicrometerMetricsCollector(meterRegistry);
    }
    
    @Nested
    @DisplayName("成功调用记录测试")
    class RecordSuccessTests {
        
        @Test
        @DisplayName("记录成功调用 - 验证所有指标")
        void testRecordSuccess_AllMetrics() {
            collector.recordSuccess("test-model", 1500L, 100, 200);
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter successCalls = meterRegistry.counter("mayfly.model.calls.success");
            Timer latencyTimer = meterRegistry.timer("mayfly.model.latency.seconds");
            DistributionSummary inputTokens = meterRegistry.summary("mayfly.model.tokens.input");
            DistributionSummary outputTokens = meterRegistry.summary("mayfly.model.tokens.output");
            
            assertEquals(1.0, totalCalls.count());
            assertEquals(1.0, successCalls.count());
            assertEquals(1, latencyTimer.count());
            assertTrue(latencyTimer.totalTime(TimeUnit.MILLISECONDS) > 0);
            assertEquals(100.0, inputTokens.totalAmount());
            assertEquals(200.0, outputTokens.totalAmount());
        }
        
        @Test
        @DisplayName("多次记录成功调用 - 指标累加")
        void testRecordSuccess_MultipleCalls_Accumulate() {
            collector.recordSuccess("model-1", 1000L, 50, 100);
            collector.recordSuccess("model-1", 2000L, 60, 120);
            collector.recordSuccess("model-2", 1500L, 70, 140);
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter successCalls = meterRegistry.counter("mayfly.model.calls.success");
            Timer latencyTimer = meterRegistry.timer("mayfly.model.latency.seconds");
            DistributionSummary inputTokens = meterRegistry.summary("mayfly.model.tokens.input");
            DistributionSummary outputTokens = meterRegistry.summary("mayfly.model.tokens.output");
            
            assertEquals(3.0, totalCalls.count());
            assertEquals(3.0, successCalls.count());
            assertEquals(3, latencyTimer.count());
            assertEquals(180.0, inputTokens.totalAmount());
            assertEquals(360.0, outputTokens.totalAmount());
        }
        
        @Test
        @DisplayName("记录零Token调用")
        void testRecordSuccess_ZeroTokens() {
            collector.recordSuccess("test-model", 800L, 0, 0);
            
            DistributionSummary inputTokens = meterRegistry.summary("mayfly.model.tokens.input");
            DistributionSummary outputTokens = meterRegistry.summary("mayfly.model.tokens.output");
            
            assertEquals(0.0, inputTokens.totalAmount());
            assertEquals(0.0, outputTokens.totalAmount());
        }
    }
    
    @Nested
    @DisplayName("失败调用记录测试")
    class RecordFailureTests {
        
        @Test
        @DisplayName("记录失败调用 - 验证指标")
        void testRecordFailure_Metrics() {
            collector.recordFailure("test-model", "SocketTimeoutException");
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter failureCalls = meterRegistry.counter("mayfly.model.calls.failure");
            
            assertEquals(1.0, totalCalls.count());
            assertEquals(1.0, failureCalls.count());
        }
        
        @Test
        @DisplayName("多次记录失败调用 - 指标累加")
        void testRecordFailure_MultipleCalls_Accumulate() {
            collector.recordFailure("model-1", "TimeoutException");
            collector.recordFailure("model-1", "ConnectionException");
            collector.recordFailure("model-2", "HttpException");
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter failureCalls = meterRegistry.counter("mayfly.model.calls.failure");
            
            assertEquals(3.0, totalCalls.count());
            assertEquals(3.0, failureCalls.count());
        }
        
        @Test
        @DisplayName("混合成功和失败调用")
        void testRecordMixed_SuccessAndFailure() {
            collector.recordSuccess("model-1", 1000L, 50, 100);
            collector.recordFailure("model-1", "TimeoutException");
            collector.recordSuccess("model-2", 2000L, 60, 120);
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter successCalls = meterRegistry.counter("mayfly.model.calls.success");
            Counter failureCalls = meterRegistry.counter("mayfly.model.calls.failure");
            
            assertEquals(3.0, totalCalls.count());
            assertEquals(2.0, successCalls.count());
            assertEquals(1.0, failureCalls.count());
        }
    }
    
    @Nested
    @DisplayName("故障转移记录测试")
    class RecordFailoverTests {
        
        @Test
        @DisplayName("记录故障转移 - 验证指标")
        void testRecordFailover_Metrics() {
            collector.recordFailover("failed-model", "backup-model");
            
            Counter failoverCalls = meterRegistry.counter("mayfly.model.calls.failover");
            
            assertEquals(1.0, failoverCalls.count());
        }
        
        @Test
        @DisplayName("多次记录故障转移 - 指标累加")
        void testRecordFailover_MultipleCalls_Accumulate() {
            collector.recordFailover("model-1", "model-2");
            collector.recordFailover("model-2", "model-3");
            collector.recordFailover("model-1", "model-3");
            
            Counter failoverCalls = meterRegistry.counter("mayfly.model.calls.failover");
            
            assertEquals(3.0, failoverCalls.count());
        }
        
        @Test
        @DisplayName("完整故障转移场景 - 失败+转移+成功")
        void testRecordFailover_CompleteScenario() {
            collector.recordFailure("primary-model", "TimeoutException");
            collector.recordFailover("primary-model", "backup-model");
            collector.recordSuccess("backup-model", 1200L, 80, 160);
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            Counter failureCalls = meterRegistry.counter("mayfly.model.calls.failure");
            Counter failoverCalls = meterRegistry.counter("mayfly.model.calls.failover");
            Counter successCalls = meterRegistry.counter("mayfly.model.calls.success");
            
            assertEquals(2.0, totalCalls.count());
            assertEquals(1.0, failureCalls.count());
            assertEquals(1.0, failoverCalls.count());
            assertEquals(1.0, successCalls.count());
        }
    }
    
    @Nested
    @DisplayName("指标名称验证测试")
    class MetricNameTests {
        
        @Test
        @DisplayName("验证所有指标名称正确注册")
        void testMetricNames_AllRegistered() {
            collector.recordSuccess("test", 100L, 10, 20);
            collector.recordFailure("test", "error");
            collector.recordFailover("from", "to");
            
            assertNotNull(meterRegistry.find("mayfly.model.calls.total").counter());
            assertNotNull(meterRegistry.find("mayfly.model.calls.success").counter());
            assertNotNull(meterRegistry.find("mayfly.model.calls.failure").counter());
            assertNotNull(meterRegistry.find("mayfly.model.calls.failover").counter());
            assertNotNull(meterRegistry.find("mayfly.model.latency.seconds").timer());
            assertNotNull(meterRegistry.find("mayfly.model.tokens.input").summary());
            assertNotNull(meterRegistry.find("mayfly.model.tokens.output").summary());
        }
    }
    
    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencyTests {
        
        @Test
        @DisplayName("多线程并发记录 - 无异常")
        void testConcurrentRecord_NoException() {
            Thread[] threads = new Thread[10];
            
            for (int i = 0; i < 10; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        collector.recordSuccess("model", 100L, 10, 20);
                    }
                });
                threads[i].start();
            }
            
            for (Thread thread : threads) {
                assertDoesNotThrow(() -> thread.join(5000));
            }
            
            Counter totalCalls = meterRegistry.counter("mayfly.model.calls.total");
            assertEquals(1000.0, totalCalls.count());
        }
    }
}
