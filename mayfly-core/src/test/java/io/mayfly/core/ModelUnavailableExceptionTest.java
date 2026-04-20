package io.mayfly.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelUnavailableException 单元测试")
class ModelUnavailableExceptionTest {
    
    @Test
    @DisplayName("测试模型不可用异常构造")
    void testExceptionConstruction() {
        ModelUnavailableException exception = new ModelUnavailableException("No models available");
        
        assertEquals("No models available", exception.getMessage());
    }
    
    @Test
    @DisplayName("测试异常继承关系")
    void testExceptionInheritance() {
        ModelUnavailableException exception = new ModelUnavailableException("Test");
        
        assertTrue(exception instanceof MayflyException);
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
    }
    
    @Test
    @DisplayName("测试异常可以被MayflyException捕获")
    void testCaughtAsMayflyException() {
        boolean caught = false;
        
        try {
            throw new ModelUnavailableException("Model down");
        } catch (MayflyException e) {
            caught = true;
            assertTrue(e instanceof ModelUnavailableException);
            assertEquals("Model down", e.getMessage());
        }
        
        assertTrue(caught);
    }
    
    @Test
    @DisplayName("测试异常可以被RuntimeException捕获")
    void testCaughtAsRuntimeException() {
        boolean caught = false;
        
        try {
            throw new ModelUnavailableException("Runtime test");
        } catch (RuntimeException e) {
            caught = true;
            assertEquals("Runtime test", e.getMessage());
        }
        
        assertTrue(caught);
    }
    
    @Test
    @DisplayName("测试异常消息包含模型信息")
    void testExceptionMessageWithModelInfo() {
        String message = "Model 'zhipu-primary' is unavailable after 3 retries";
        ModelUnavailableException exception = new ModelUnavailableException(message);
        
        assertTrue(exception.getMessage().contains("zhipu-primary"));
        assertTrue(exception.getMessage().contains("unavailable"));
    }
    
    @Test
    @DisplayName("测试多个异常的独立性")
    void testMultipleExceptionsIndependence() {
        ModelUnavailableException ex1 = new ModelUnavailableException("Error 1");
        ModelUnavailableException ex2 = new ModelUnavailableException("Error 2");
        
        assertNotSame(ex1, ex2);
        assertEquals("Error 1", ex1.getMessage());
        assertEquals("Error 2", ex2.getMessage());
    }
}
