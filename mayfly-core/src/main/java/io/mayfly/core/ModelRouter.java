package io.mayfly.core;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * 模型路由核心接口
 * 所有模型调用的统一入口
 */
public interface ModelRouter {
    
    /**
     * 同步调用
     * @param prompt 聊天提示
     * @return 聊天响应
     */
    ChatResponse chat(Prompt prompt);
    
    /**
     * 流式调用
     * @param prompt 聊天提示
     * @return 聊天响应流
     */
    Flux<ChatResponse> stream(Prompt prompt);
    
    /**
     * 异步调用
     * @param prompt 聊天提示
     * @return 聊天响应 Future
     */
    CompletableFuture<ChatResponse> async(Prompt prompt);
}
