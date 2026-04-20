package io.mayfly.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseHttpClient 单元测试
 */
@DisplayName("BaseHttpClient 测试")
class BaseHttpClientTest {

    private TestableBaseHttpClient client;

    @BeforeEach
    void setUp() {
        client = new TestableBaseHttpClient("test-api-key", "https://api.test.com", "test-model");
    }

    @Test
    @DisplayName("构造函数初始化字段")
    void testConstructorInitialization() {
        assertEquals("test-api-key", client.getApiKey());
        assertEquals("https://api.test.com", client.getBaseUrl());
        assertEquals("test-model", client.getModel());
        assertNotNull(client.getRestTemplate());
        assertNotNull(client.getObjectMapper());
    }

    @Test
    @DisplayName("createHeaders 创建正确的请求头")
    void testCreateHeaders() {
        HttpHeaders headers = client.testCreateHeaders();

        assertNotNull(headers);
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("Bearer test-api-key", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("createRequestEntity 创建正确的请求实体")
    void testCreateRequestEntity() throws Exception {
        Map<String, Object> requestBody = Map.of(
            "model", "test-model",
            "messages", "test-message"
        );

        HttpEntity<String> entity = client.testCreateRequestEntity(requestBody);

        assertNotNull(entity);
        assertNotNull(entity.getBody());
        assertTrue(entity.getBody().contains("\"model\":\"test-model\""));
        assertTrue(entity.getBody().contains("\"messages\":\"test-message\""));
        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
        assertEquals("Bearer test-api-key", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("createRequestEntity 处理复杂对象")
    void testCreateRequestEntityWithComplexObject() throws Exception {
        TestRequestObject requestObj = new TestRequestObject();
        requestObj.setModel("gpt-4");
        requestObj.setTemperature(0.8);
        requestObj.setMaxTokens(1024);

        HttpEntity<String> entity = client.testCreateRequestEntity(requestObj);

        assertNotNull(entity);
        String body = entity.getBody();
        assertTrue(body.contains("\"model\":\"gpt-4\""));
        assertTrue(body.contains("\"temperature\":0.8"));
        assertTrue(body.contains("\"maxTokens\":1024"));
    }

    @Test
    @DisplayName("createRequestEntity 处理 JSON 序列化异常")
    void testCreateRequestEntitySerializationError() {
        // 创建一个无法序列化的对象（循环引用）
        CyclicObject cyclic = new CyclicObject();
        cyclic.setSelf(cyclic);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.testCreateRequestEntity(cyclic);
        });

        assertTrue(exception.getMessage().contains("Failed to serialize request body"));
    }

    @Test
    @DisplayName("createRequestEntity 处理 null 对象")
    void testCreateRequestEntityWithNull() throws Exception {
        HttpEntity<String> entity = client.testCreateRequestEntity(null);

        assertNotNull(entity);
        assertEquals("null", entity.getBody());
    }

    /**
     * 可测试的具体实现类
     */
    static class TestableBaseHttpClient extends BaseHttpClient {

        public TestableBaseHttpClient(String apiKey, String baseUrl, String model) {
            super(apiKey, baseUrl, model);
        }

        public RestTemplate getRestTemplate() {
            return restTemplate;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getModel() {
            return model;
        }

        public HttpHeaders testCreateHeaders() {
            return createHeaders();
        }

        public HttpEntity<String> testCreateRequestEntity(Object requestBody) {
            return createRequestEntity(requestBody);
        }
    }

    /**
     * 测试用请求对象
     */
    static class TestRequestObject {
        private String model;
        private Double temperature;
        private Integer maxTokens;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    }

    /**
     * 循环引用对象，用于测试序列化异常
     */
    static class CyclicObject {
        private CyclicObject self;

        public CyclicObject getSelf() { return self; }
        public void setSelf(CyclicObject self) { this.self = self; }
    }
}
