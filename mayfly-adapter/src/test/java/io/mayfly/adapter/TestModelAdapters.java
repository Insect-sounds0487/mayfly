package io.mayfly.adapter;

import io.mayfly.adapter.impl.*;
import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型适配器测试类
 * 验证所有模型适配器的创建功能
 */
public class TestModelAdapters {

    private ModelConfig mockConfig;

    @BeforeEach
    void setUp() {
        // 创建模拟配置，但不设置真实的API密钥
        mockConfig = ModelConfig.builder()
            .apiKey("dummy-key-for-testing")  // 使用虚拟密钥进行测试
            .baseUrl("https://test.example.com")
            .model("test-model")
            .build();
    }

    @Test
    void testClaudeModelAdapterCreation() {
        ClaudeModelAdapter adapter = new ClaudeModelAdapter();
        assertEquals("claude", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testZhipuModelAdapterCreation() {
        ZhipuModelAdapter adapter = new ZhipuModelAdapter();
        assertEquals("zhipu", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testDeepSeekModelAdapterCreation() {
        DeepSeekModelAdapter adapter = new DeepSeekModelAdapter();
        assertEquals("deepseek", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testTongyiModelAdapterCreation() {
        TongyiModelAdapter adapter = new TongyiModelAdapter();
        assertEquals("tongyi", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testOpenAiModelAdapterCreation() {
        OpenAiModelAdapter adapter = new OpenAiModelAdapter();
        assertEquals("openai", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testWenxinModelAdapterCreation() {
        WenxinModelAdapter adapter = new WenxinModelAdapter();
        assertEquals("wenxin", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }

    @Test
    void testXinghuoModelAdapterCreation() {
        XinghuoModelAdapter adapter = new XinghuoModelAdapter();
        assertEquals("xinghuo", adapter.getProvider());
        
        // 测试配置验证
        assertThrows(IllegalArgumentException.class, () -> {
            ModelConfig invalidConfig = ModelConfig.builder().build();
            adapter.createChatModel(invalidConfig);
        });
    }
}