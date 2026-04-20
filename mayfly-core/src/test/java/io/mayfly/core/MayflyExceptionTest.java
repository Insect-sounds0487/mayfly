package io.mayfly.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MayflyException 单元测试")
class MayflyExceptionTest {
    
    @Test
    @DisplayName("测试仅消息的异常构造")
    void testExceptionWithMessageOnly() {
        MayflyException exception = new MayflyException("Test error message");
        
        assertEquals("Test error message", exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("测试带原因的异常构造")
    void testExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        MayflyException exception = new MayflyException("Wrapper message", cause);
        
        assertEquals("Wrapper message", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("Root cause", exception.getCause().getMessage());
    }
    
    @Test
    @DisplayName("测试异常类型为RuntimeException子类")
    void testExceptionIsRuntimeException() {
        MayflyException exception = new MayflyException("Test");
        
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    @DisplayName("测试异常可以被catch捕获")
    void testExceptionCanBeCaught() {
        boolean caught = false;
        
        try {
            throw new MayflyException("Test exception");
        } catch (MayflyException e) {
            caught = true;
            assertEquals("Test exception", e.getMessage());
        }
        
        assertTrue(caught);
    }
    
    @Test
    @DisplayName("测试异常链传递")
    void testExceptionChain() {
        IllegalArgumentException rootCause = new IllegalArgumentException("Invalid argument");
        MayflyException wrapper = new MayflyException("Mayfly error", rootCause);
        
        MayflyException outer = new MayflyException("Outer error", wrapper);
        
        assertEquals("Outer error", outer.getMessage());
        assertSame(wrapper, outer.getCause());
        assertSame(rootCause, outer.getCause().getCause());
    }
    
    @Test
    @DisplayName("测试空消息异常")
    void testExceptionWithNullMessage() {
        MayflyException exception = new MayflyException(null);
        
        assertNull(exception.getMessage());
    }
    
    @Test
    @DisplayName("测试空字符串消息异常")
    void testExceptionWithEmptyMessage() {
        MayflyException exception = new MayflyException("");
        
        assertEquals("", exception.getMessage());
    }
    
    @Test
    @DisplayName("测试异常堆栈跟踪")
    void testExceptionStackTrace() {
        MayflyException exception = new MayflyException("Stack trace test");
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }
}
