package io.mayfly.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * 国产模型 HTTP 客户端基类
 */
public abstract class BaseHttpClient {
    
    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;
    protected final String apiKey;
    protected final String baseUrl;
    protected final String model;
    
    public BaseHttpClient(String apiKey, String baseUrl, String model) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }
    
    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }
    
    protected HttpEntity<String> createRequestEntity(Object requestBody) {
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            return new HttpEntity<>(jsonBody, createHeaders());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }
}