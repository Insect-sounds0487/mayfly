package io.mayfly.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelConfig 单元测试")
class ModelConfigTest {
    
    @Test
    @DisplayName("测试使用Builder创建完整配置")
    void testBuilderWithAllFields() {
        ModelConfig config = ModelConfig.builder()
            .name("test-model")
            .provider("zhipu")
            .model("glm-4")
            .apiKey("test-api-key")
            .baseUrl("https://api.example.com")
            .weight(80)
            .enabled(true)
            .timeout(60000)
            .maxRetries(3)
            .build();
        
        assertEquals("test-model", config.getName());
        assertEquals("zhipu", config.getProvider());
        assertEquals("glm-4", config.getModel());
        assertEquals("test-api-key", config.getApiKey());
        assertEquals("https://api.example.com", config.getBaseUrl());
        assertEquals(80, config.getWeight());
        assertTrue(config.isEnabled());
        assertEquals(60000, config.getTimeout());
        assertEquals(3, config.getMaxRetries());
        assertNotNull(config.getTags());
        assertTrue(config.getTags().isEmpty());
        assertNotNull(config.getProperties());
        assertTrue(config.getProperties().isEmpty());
    }
    
    @Test
    @DisplayName("测试使用默认值创建配置")
    void testBuilderWithDefaults() {
        ModelConfig config = ModelConfig.builder()
            .name("default-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .build();
        
        assertEquals(100, config.getWeight());
        assertTrue(config.isEnabled());
        assertEquals(30000, config.getTimeout());
        assertEquals(2, config.getMaxRetries());
        assertNull(config.getBaseUrl());
    }
    
    @Test
    @DisplayName("测试使用NoArgsConstructor创建配置")
    void testNoArgsConstructor() {
        ModelConfig config = new ModelConfig();
        
        assertNull(config.getName());
        assertNull(config.getProvider());
        assertEquals(100, config.getWeight());
        assertTrue(config.isEnabled());
        assertEquals(30000, config.getTimeout());
        assertEquals(2, config.getMaxRetries());
        assertNotNull(config.getTags());
        assertNotNull(config.getProperties());
    }
    
    @Test
    @DisplayName("测试使用AllArgsConstructor创建配置")
    void testAllArgsConstructor() {
        ModelConfig config = new ModelConfig(
            "full-model",
            "openai",
            "gpt-4",
            "sk-test",
            "https://api.openai.com",
            50,
            false,
            45000,
            5,
            null,
            null
        );
        
        assertEquals("full-model", config.getName());
        assertEquals("openai", config.getProvider());
        assertEquals("gpt-4", config.getModel());
        assertEquals("sk-test", config.getApiKey());
        assertEquals("https://api.openai.com", config.getBaseUrl());
        assertEquals(50, config.getWeight());
        assertFalse(config.isEnabled());
        assertEquals(45000, config.getTimeout());
        assertEquals(5, config.getMaxRetries());
    }
    
    @Test
    @DisplayName("测试Setter和Getter")
    void testSettersAndGetters() {
        ModelConfig config = new ModelConfig();
        
        config.setName("updated-model");
        config.setProvider("tongyi");
        config.setModel("qwen-max");
        config.setApiKey("new-key");
        config.setBaseUrl("https://dashscope.aliyuncs.com");
        config.setWeight(90);
        config.setEnabled(false);
        config.setTimeout(20000);
        config.setMaxRetries(1);
        
        assertEquals("updated-model", config.getName());
        assertEquals("tongyi", config.getProvider());
        assertEquals("qwen-max", config.getModel());
        assertEquals("new-key", config.getApiKey());
        assertEquals("https://dashscope.aliyuncs.com", config.getBaseUrl());
        assertEquals(90, config.getWeight());
        assertFalse(config.isEnabled());
        assertEquals(20000, config.getTimeout());
        assertEquals(1, config.getMaxRetries());
    }
    
    @Test
    @DisplayName("测试Tags操作")
    void testTagsManipulation() {
        ModelConfig config = ModelConfig.builder()
            .name("tagged-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .build();
        
        config.getTags().add("production");
        config.getTags().add("high-priority");
        
        assertEquals(2, config.getTags().size());
        assertTrue(config.getTags().contains("production"));
        assertTrue(config.getTags().contains("high-priority"));
    }
    
    @Test
    @DisplayName("测试Properties操作")
    void testPropertiesManipulation() {
        ModelConfig config = ModelConfig.builder()
            .name("property-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .build();
        
        config.getProperties().put("temperature", 0.7);
        config.getProperties().put("max_tokens", 2000);
        
        assertEquals(2, config.getProperties().size());
        assertEquals(0.7, config.getProperties().get("temperature"));
        assertEquals(2000, config.getProperties().get("max_tokens"));
    }
    
    @Test
    @DisplayName("测试带Tags的Builder")
    void testBuilderWithTags() {
        ModelConfig config = ModelConfig.builder()
            .name("tagged-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .tags(java.util.Arrays.asList("prod", "critical"))
            .build();
        
        assertEquals(2, config.getTags().size());
        assertTrue(config.getTags().contains("prod"));
        assertTrue(config.getTags().contains("critical"));
    }
    
    @Test
    @DisplayName("测试带Properties的Builder")
    void testBuilderWithProperties() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("temperature", 0.5);
        props.put("top_p", 0.9);
        
        ModelConfig config = ModelConfig.builder()
            .name("property-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .properties(props)
            .build();
        
        assertEquals(2, config.getProperties().size());
        assertEquals(0.5, config.getProperties().get("temperature"));
        assertEquals(0.9, config.getProperties().get("top_p"));
    }
    
    @Test
    @DisplayName("测试禁用状态的模型配置")
    void testDisabledModelConfig() {
        ModelConfig config = ModelConfig.builder()
            .name("disabled-model")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .enabled(false)
            .build();
        
        assertFalse(config.isEnabled());
    }
    
    @Test
    @DisplayName("测试自定义超时和重试配置")
    void testCustomTimeoutAndRetries() {
        ModelConfig config = ModelConfig.builder()
            .name("custom-config")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .timeout(120000)
            .maxRetries(5)
            .build();
        
        assertEquals(120000, config.getTimeout());
        assertEquals(5, config.getMaxRetries());
    }
    
    @Test
    @DisplayName("测试权重边界值")
    void testWeightBoundaryValues() {
        ModelConfig zeroWeight = ModelConfig.builder()
            .name("zero-weight")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .weight(0)
            .build();
        
        assertEquals(0, zeroWeight.getWeight());
        
        ModelConfig highWeight = ModelConfig.builder()
            .name("high-weight")
            .provider("test")
            .model("test-v1")
            .apiKey("test-key")
            .weight(1000)
            .build();
        
        assertEquals(1000, highWeight.getWeight());
    }
}
