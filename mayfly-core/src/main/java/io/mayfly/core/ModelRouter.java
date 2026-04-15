package io.mayfly.core;

import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * 模型路由核心接口
 * 所有模型调用的统一入口
 */
public interface ModelRouter {
    
    /**
     * 同步调用
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 流式调用
     * @param request 聊天请求
     * @return 聊天响应流
     */
    Flux<ChatResponse> stream(ChatRequest request);
    
    /**
     * 异步调用
     * @param request 聊天请求
     * @return 聊天响应Future
     */
    CompletableFuture<ChatResponse> async(ChatRequest request);
}
