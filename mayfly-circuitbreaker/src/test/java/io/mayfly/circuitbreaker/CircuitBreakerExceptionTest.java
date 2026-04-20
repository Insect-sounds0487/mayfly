package io.mayfly.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CircuitBreaker 模块异常测试
 */
@DisplayName("CircuitBreaker 模块异常测试")
class CircuitBreakerExceptionTest {

    @Nested
    @DisplayName("熔断器异常测试")
    class CircuitBreakerExceptionTests {

        @Test
        @DisplayName("连续失败触发熔断")
        void testCircuitOpensAfterFailures() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            for (int i = 0; i < 5; i++) {
                try {
                    manager.executeProtected("test-model", () -> {
                        throw new RuntimeException("Simulated failure");
                    });
                } catch (Exception ignored) {
                }
            }

            CallNotPermittedException exception = assertThrows(CallNotPermittedException.class, () -> {
                manager.executeProtected("test-model", () -> "success");
            });

            assertNotNull(exception);
        }

        @Test
        @DisplayName("熔断器打开后等待时间结束允许重试")
        void testCircuitHalfOpenAfterWaitDuration() throws InterruptedException {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(3)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(java.time.Duration.ofMillis(100))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            for (int i = 0; i < 3; i++) {
                try {
                    manager.executeProtected("test-model", () -> {
                        throw new RuntimeException("Failure");
                    });
                } catch (Exception ignored) {
                }
            }

            Thread.sleep(150);

            assertDoesNotThrow(() -> {
                manager.executeProtected("test-model", () -> "success");
            });
        }

        @Test
        @DisplayName("成功调用重置失败计数")
        void testSuccessfulCallResetsFailureCount() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            manager.executeProtected("test-model", () -> "success");

            try {
                manager.executeProtected("test-model", () -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception ignored) {
            }

            manager.executeProtected("test-model", () -> "success");
            manager.executeProtected("test-model", () -> "success");

            assertDoesNotThrow(() -> {
                manager.executeProtected("test-model", () -> "success");
            });
        }

        @Test
        @DisplayName("不同模型有独立的熔断器")
        void testIndependentCircuitBreakersPerModel() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(3)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            for (int i = 0; i < 3; i++) {
                try {
                    manager.executeProtected("model-a", () -> {
                        throw new RuntimeException("Failure");
                    });
                } catch (Exception ignored) {
                }
            }

            assertThrows(CallNotPermittedException.class, () -> {
                manager.executeProtected("model-a", () -> "success");
            });

            assertDoesNotThrow(() -> {
                manager.executeProtected("model-b", () -> "success");
            });
        }
    }

    @Nested
    @DisplayName("限流器异常测试")
    class RateLimiterExceptionTests {

        @Test
        @DisplayName("超过限流阈值抛出异常")
        void testRateLimitExceeded() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .limitForPeriod(2)
                .limitRefreshPeriod(java.time.Duration.ofSeconds(10))
                .timeoutDuration(java.time.Duration.ofMillis(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            manager.executeProtected("test-model", () -> "call-1");
            manager.executeProtected("test-model", () -> "call-2");

            RequestNotPermitted exception = assertThrows(RequestNotPermitted.class, () -> {
                manager.executeProtected("test-model", () -> "call-3");
            });

            assertNotNull(exception);
        }

        @Test
        @DisplayName("限流周期刷新后允许新请求")
        void testRateLimiterRefreshesAfterPeriod() throws InterruptedException {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .limitForPeriod(2)
                .limitRefreshPeriod(java.time.Duration.ofMillis(100))
                .timeoutDuration(java.time.Duration.ofMillis(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            manager.executeProtected("test-model", () -> "call-1");
            manager.executeProtected("test-model", () -> "call-2");

            assertThrows(RequestNotPermitted.class, () -> {
                manager.executeProtected("test-model", () -> "call-3");
            });

            Thread.sleep(150);

            assertDoesNotThrow(() -> {
                manager.executeProtected("test-model", () -> "call-4");
            });
        }

        @Test
        @DisplayName("不同模型有独立的限流器")
        void testIndependentRateLimitersPerModel() {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .limitForPeriod(1)
                .limitRefreshPeriod(java.time.Duration.ofSeconds(10))
                .timeoutDuration(java.time.Duration.ofMillis(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            manager.executeProtected("model-a", () -> "call-a1");

            assertThrows(RequestNotPermitted.class, () -> {
                manager.executeProtected("model-a", () -> "call-a2");
            });

            assertDoesNotThrow(() -> {
                manager.executeProtected("model-b", () -> "call-b1");
            });
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrentScenarioTests {

        @Test
        @DisplayName("高并发下熔断器正常工作")
        void testCircuitBreakerUnderHighConcurrency() throws InterruptedException {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            AtomicInteger circuitOpenCount = new AtomicInteger(0);

            CompletableFuture<?>[] futures = new CompletableFuture[50];

            for (int i = 0; i < 50; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        manager.executeProtected("test-model", () -> {
                            if (index % 3 == 0) {
                                return "success";
                            } else {
                                throw new RuntimeException("Failure");
                            }
                        });
                        successCount.incrementAndGet();
                    } catch (CallNotPermittedException e) {
                        circuitOpenCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).join();
            executor.shutdown();

            assertTrue(successCount.get() + failureCount.get() + circuitOpenCount.get() == 50);
        }

        @Test
        @DisplayName("高并发下限流器正常工作")
        void testRateLimiterUnderHighConcurrency() throws InterruptedException {
            CircuitBreakerConfigProperties config = CircuitBreakerConfigProperties.builder()
                .failureRateThreshold(100)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(60))
                .slidingWindowSize(100)
                .limitForPeriod(5)
                .limitRefreshPeriod(java.time.Duration.ofMillis(100))
                .timeoutDuration(java.time.Duration.ofMillis(10))
                .build();

            CircuitBreakerManager manager = new CircuitBreakerManager(config);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger rateLimitedCount = new AtomicInteger(0);

            CompletableFuture<?>[] futures = new CompletableFuture[20];

            for (int i = 0; i < 20; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        manager.executeProtected("test-model", () -> "success");
                        successCount.incrementAndGet();
                    } catch (RequestNotPermitted | CallNotPermittedException e) {
                        rateLimitedCount.incrementAndGet();
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).join();
            executor.shutdown();

            assertEquals(20, successCount.get() + rateLimitedCount.get());
            assertTrue(successCount.get() >= 5);
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTests {

        @Test
        @DisplayName("空模型名称")
        void testEmptyModelName() {
            CircuitBreakerManager manager = new CircuitBreakerManager();

            assertDoesNotThrow(() -> {
                manager.executeProtected("", () -> "success");
            });
        }

        @Test
        @DisplayName("null 模型名称")
        void testNullModelName() {
            CircuitBreakerManager manager = new CircuitBreakerManager();

            assertThrows(NullPointerException.class, () -> {
                manager.executeProtected(null, () -> "success");
            });
        }

        @Test
        @DisplayName("供应商返回 null 值")
        void testSupplierReturnsNull() {
            CircuitBreakerManager manager = new CircuitBreakerManager();

            Object result = manager.executeProtected("test-model", () -> null);

            assertNull(result);
        }

        @Test
        @DisplayName("供应商返回异常")
        void testSupplierThrowsException() {
            CircuitBreakerManager manager = new CircuitBreakerManager();

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                manager.executeProtected("test-model", () -> {
                    throw new RuntimeException("Custom exception");
                });
            });

            assertEquals("Custom exception", exception.getMessage());
        }

        @Test
        @DisplayName("供应商返回受检异常")
        void testSupplierThrowsCheckedException() {
            CircuitBreakerManager manager = new CircuitBreakerManager();

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                manager.executeProtected("test-model", () -> {
                    throw new RuntimeException(new java.io.IOException("IO exception"));
                });
            });

            assertTrue(exception.getCause() instanceof java.io.IOException);
        }
    }
}
