package io.mayfly.demo.controller;

import io.mayfly.core.ModelRouter;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 * 演示如何使用 Mayfly ModelRouter 进行模型调用
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final ModelRouter modelRouter;
    
    public ChatController(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }
    
    /**
     * 简单聊天接口
     */
    @PostMapping("/send")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        Prompt prompt = new Prompt(new UserMessage(request.getMessage()));
        ChatResponse response = modelRouter.chat(prompt);
        
        return Map.of(
            "success", true,
            "message", response.getResult().getOutput().getText(),
            "model", response.getMetadata() != null ? response.getMetadata().getModel() : "unknown"
        );
    }
    
    /**
     * 带元数据的聊天接口（用于测试规则路由）
     */
    @PostMapping("/send-with-metadata")
    public Map<String, Object> chatWithMetadata(@RequestBody ChatRequestWithMetadata request) {
        Prompt prompt = new Prompt(
            List.of(new UserMessage(request.getMessage())),
            null
        );
        ChatResponse response = modelRouter.chat(prompt);
        
        return Map.of(
            "success", true,
            "message", response.getResult().getOutput().getText(),
            "model", response.getMetadata() != null ? response.getMetadata().getModel() : "unknown"
        );
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "mayfly-demo"
        );
    }
    
    /**
     * 聊天请求
     */
    public static class ChatRequest {
        private String message;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 带元数据的聊天请求
     */
    public static class ChatRequestWithMetadata {
        private String message;
        private Map<String, Object> metadata;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
