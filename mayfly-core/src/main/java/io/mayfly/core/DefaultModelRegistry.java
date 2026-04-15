package io.mayfly.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatRequest;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 默认模型注册中心实现
 */
@Slf4j
public class DefaultModelRegistry implements ModelRegistry {
    
    private final Map<String, ModelInstance> modelInstances = new ConcurrentHashMap<>();
    private final List<ModelAdapter> adapters;
    
    public DefaultModelRegistry(MayflyProperties properties, List<ModelAdapter> adapters) {
        this.adapters = adapters != null ? adapters : Collections.emptyList();
        
        if (properties != null && properties.getModels() != null) {
            for (ModelConfig config : properties.getModels()) {
                register(config);
            }
        }
    }
    
    @Override
    public void register(ModelConfig config) {
        if (config == null || config.getName() == null) {
            throw new IllegalArgumentException("Model config and name must not be null");
        }
        
        ModelAdapter adapter = findAdapter(config.getProvider());
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for provider: " + config.getProvider());
        }
        
        ChatModel chatModel = adapter.createChatModel(config);
        ModelInstance instance = new ModelInstance(config, chatModel);
        modelInstances.put(config.getName(), instance);
        
        log.info("Registered model: {} (provider: {})", config.getName(), config.getProvider());
    }
    
    @Override
    public Optional<ModelInstance> getModel(String name) {
        return Optional.ofNullable(modelInstances.get(name));
    }
    
    @Override
    public List<ModelInstance> getAllAvailableModels() {
        return modelInstances.values().stream()
            .filter(ModelInstance::isAvailable)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ModelInstance> getAllModels() {
        return new ArrayList<>(modelInstances.values());
    }
    
    @Override
    public void updateModel(String name, ModelConfig config) {
        modelInstances.remove(name);
        register(config);
        log.info("Updated model: {}", name);
    }
    
    @Override
    public void removeModel(String name) {
        modelInstances.remove(name);
        log.info("Removed model: {}", name);
    }
    
    @Override
    public List<ModelInstance> getModelsByTag(String tag) {
        return modelInstances.values().stream()
            .filter(m -> m.getConfig().getTags() != null && 
                        m.getConfig().getTags().contains(tag))
            .collect(Collectors.toList());
    }
    
    private ModelAdapter findAdapter(String provider) {
        if (adapters == null) {
            return null;
        }
        return adapters.stream()
            .filter(a -> a.getProvider().equalsIgnoreCase(provider))
            .findFirst()
            .orElse(null);
    }
}
