package io.mayfly.autoconfigure;

import io.mayfly.circuitbreaker.CircuitBreakerManager;
import io.mayfly.core.*;
import io.mayfly.failover.FailoverHandler;
import io.mayfly.failover.FailoverResult;
import io.mayfly.monitor.MetricsCollector;
import io.mayfly.router.RouterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 默认模型路由器实现
 */
@Slf4j
public class DefaultModelRouter implements ModelRouter {
    
    private final ModelRegistry modelRegistry;
    private final RouterStrategy routerStrategy;
    private final FailoverHandler failoverHandler;
    private final CircuitBreakerManager circuitBreakerManager;
    private final MetricsCollector metricsCollector;
    
    public DefaultModelRouter(ModelRegistry modelRegistry,
                              RouterStrategy routerStrategy,
                              FailoverHandler failoverHandler,
                              CircuitBreakerManager circuitBreakerManager,
                              MetricsCollector metricsCollector) {
        this.modelRegistry = modelRegistry;
        this.routerStrategy = routerStrategy;
        this.failoverHandler = failoverHandler;
        this.circuitBreakerManager = circuitBreakerManager;
        this.metricsCollector = metricsCollector;
    }
    
    @Override
    public ChatResponse chat(Prompt request) {
        long startTime = System.currentTimeMillis();
        List<ModelInstance> candidates = modelRegistry.getAllAvailableModels();
        
        if (candidates.isEmpty()) {
            throw new ModelUnavailableException("No available models");
        }
        
        ModelInstance target = routerStrategy.select(request, candidates);
        target.getActiveRequests().incrementAndGet();
        
        try {
            ChatResponse response = circuitBreakerManager.executeProtected(
                target.getConfig().getName(),
                () -> target.getChatModel().call(request)
            );
            
            long latency = System.currentTimeMillis() - startTime;
            target.recordSuccess(latency);
            
            if (metricsCollector != null) {
                metricsCollector.recordSuccess(target.getConfig().getName(), latency, 0, 0);
            }
            
            log.debug("Model {} responded in {}ms", target.getConfig().getName(), latency);
            return response;
            
        } catch (Exception e) {
            target.recordFailure();
            long latency = System.currentTimeMillis() - startTime;
            
            log.error("Model {} call failed after {}ms: {}", 
                target.getConfig().getName(), latency, e.getMessage(), e);
            
            if (metricsCollector != null) {
                metricsCollector.recordFailure(target.getConfig().getName(), e.getClass().getSimpleName());
            }
            
            if (failoverHandler.isRetryable(e)) {
                FailoverResult result = failoverHandler.executeFailover(
                    request, target, candidates, e);
                
                if (result.isSuccess()) {
                    if (metricsCollector != null) {
                        metricsCollector.recordFailover(
                            target.getConfig().getName(), 
                            result.getTargetModel().getConfig().getName());
                    }
                    return chatWithModel(request, result.getTargetModel());
                }
            }
            
            throw new MayflyException("Model call failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<ChatResponse> stream(Prompt request) {
        List<ModelInstance> candidates = modelRegistry.getAllAvailableModels();
        
        if (candidates.isEmpty()) {
            return Flux.error(new ModelUnavailableException("No available models"));
        }
        
        ModelInstance target = routerStrategy.select(request, candidates);
        target.getActiveRequests().incrementAndGet();
        
        return Flux.from(target.getChatModel().stream(request))
            .doOnComplete(() -> target.getActiveRequests().decrementAndGet())
            .doOnError(e -> {
                target.recordFailure();
                target.getActiveRequests().decrementAndGet();
            });
    }
    
    @Override
    public CompletableFuture<ChatResponse> async(Prompt request) {
        return CompletableFuture.supplyAsync(() -> chat(request));
    }
    
    private ChatResponse chatWithModel(Prompt request, ModelInstance model) {
        model.getActiveRequests().incrementAndGet();
        try {
            return circuitBreakerManager.executeProtected(
                model.getConfig().getName(),
                () -> model.getChatModel().call(request)
            );
        } finally {
            model.getActiveRequests().decrementAndGet();
        }
    }
}
