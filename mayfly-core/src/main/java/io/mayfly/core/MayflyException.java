package io.mayfly.core;

/**
 * Mayfly异常基类
 */
public class MayflyException extends RuntimeException {
    
    public MayflyException(String message) {
        super(message);
    }
    
    public MayflyException(String message, Throwable cause) {
        super(message, cause);
    }
}
