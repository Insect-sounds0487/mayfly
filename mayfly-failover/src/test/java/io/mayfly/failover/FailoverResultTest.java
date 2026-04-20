package io.mayfly.failover;

import io.mayfly.core.ModelConfig;
import io.mayfly.core.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FailoverResult 单元测试")
class FailoverResultTest {
    
    private ModelInstance backupModel;
    
    @BeforeEach
    void setUp() {
        ModelConfig config = ModelConfig.builder()
            .name("backup-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .build();
        
        backupModel = new ModelInstance(config, null);
    }
    
    @Nested
    @DisplayName("成功结果测试")
    class SuccessResultTests {
        
        @Test
        @DisplayName("测试创建成功结果")
        void testCreateSuccessResult() {
            FailoverResult result = FailoverResult.success(backupModel);
            
            assertTrue(result.isSuccess());
            assertSame(backupModel, result.getTargetModel());
            assertNull(result.getErrorMessage());
        }
        
        @Test
        @DisplayName("测试成功结果的目标模型")
        void testSuccessResultTargetModel() {
            ModelConfig config2 = ModelConfig.builder()
                .name("another-backup")
                .provider("test")
                .model("test-v2")
                .apiKey("test-key")
                .build();
            
            ModelInstance anotherBackup = new ModelInstance(config2, null);
            FailoverResult result = FailoverResult.success(anotherBackup);
            
            assertSame(anotherBackup, result.getTargetModel());
            assertEquals("another-backup", result.getTargetModel().getConfig().getName());
        }
    }
    
    @Nested
    @DisplayName("失败结果测试")
    class FailureResultTests {
        
        @Test
        @DisplayName("测试创建失败结果")
        void testCreateFailureResult() {
            FailoverResult result = FailoverResult.failure("No backup available");
            
            assertFalse(result.isSuccess());
            assertNull(result.getTargetModel());
            assertEquals("No backup available", result.getErrorMessage());
        }
        
        @Test
        @DisplayName("测试失败结果的错误消息")
        void testFailureResultErrorMessage() {
            String errorMessage = "All backup models are in cooldown period";
            FailoverResult result = FailoverResult.failure(errorMessage);
            
            assertEquals(errorMessage, result.getErrorMessage());
        }
        
        @Test
        @DisplayName("测试失败结果的目标模型为null")
        void testFailureResultTargetIsNull() {
            FailoverResult result = FailoverResult.failure("Error occurred");
            
            assertNull(result.getTargetModel());
        }
    }
    
    @Nested
    @DisplayName("结果属性测试")
    class ResultPropertyTests {
        
        @Test
        @DisplayName("测试成功结果的success属性为true")
        void testSuccessResultSuccessProperty() {
            FailoverResult result = FailoverResult.success(backupModel);
            
            assertTrue(result.isSuccess());
        }
        
        @Test
        @DisplayName("测试失败结果的success属性为false")
        void testFailureResultSuccessProperty() {
            FailoverResult result = FailoverResult.failure("Error");
            
            assertFalse(result.isSuccess());
        }
        
        @Test
        @DisplayName("测试成功和失败结果的独立性")
        void testSuccessAndFailureIndependence() {
            FailoverResult successResult = FailoverResult.success(backupModel);
            FailoverResult failureResult = FailoverResult.failure("Error");
            
            assertNotSame(successResult, failureResult);
            assertTrue(successResult.isSuccess());
            assertFalse(failureResult.isSuccess());
        }
    }
    
    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTests {
        
        @Test
        @DisplayName("测试success工厂方法")
        void testSuccessFactoryMethod() {
            FailoverResult result = FailoverResult.success(backupModel);
            
            assertTrue(result.isSuccess());
            assertSame(backupModel, result.getTargetModel());
            assertNull(result.getErrorMessage());
        }
        
        @Test
        @DisplayName("测试failure工厂方法")
        void testFailureFactoryMethod() {
            FailoverResult result = FailoverResult.failure("Error occurred");
            
            assertFalse(result.isSuccess());
            assertNull(result.getTargetModel());
            assertEquals("Error occurred", result.getErrorMessage());
        }
    }
}
