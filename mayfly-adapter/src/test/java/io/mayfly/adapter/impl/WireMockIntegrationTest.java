package io.mayfly.adapter.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock 集成测试
 * 模拟真实 HTTP 服务验证适配器端到端调用
 */
@DisplayName("WireMock 集成测试")
class WireMockIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private ZhipuModelAdapter.ZhipuChatModel createChatModelWithRealHttp(String baseUrl) {
        ZhipuModelAdapter testAdapter = new ZhipuModelAdapter() {
            @Override
            protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
                return new RealHttpClient(baseUrl);
            }
        };

        return (ZhipuModelAdapter.ZhipuChatModel) testAdapter.createChatModel(ModelConfig.builder()
            .name("zhipu-wiremock")
            .provider("zhipu")
            .model("glm-4")
            .apiKey("test-api-key")
            .baseUrl(baseUrl)
            .build());
    }

    @Test
    @DisplayName("模拟智谱 API 成功响应")
    void testZhipuApiSuccessResponse() {
        String baseUrl = "http://localhost:" + wireMock.getPort() + "/api/paas/v4";

        wireMock.stubFor(post(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .withHeader("Authorization", equalToPattern("Bearer test-api-key"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                    "  \"id\": \"chatcmpl-wiremock-123\",\n" +
                    "  \"choices\": [{\n" +
                    "    \"index\": 0,\n" +
                    "    \"message\": {\n" +
                    "      \"role\": \"assistant\",\n" +
                    "      \"content\": \"Hello from WireMock!\"\n" +
                    "    },\n" +
                    "    \"finish_reason\": \"stop\"\n" +
                    "  }],\n" +
                    "  \"usage\": {\n" +
                    "    \"prompt_tokens\": 10,\n" +
                    "    \"completion_tokens\": 20,\n" +
                    "    \"total_tokens\": 30\n" +
                    "  }\n" +
                    "}")));

        ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModelWithRealHttp(baseUrl);

        Prompt prompt = new Prompt(new UserMessage("Hello"));
        var response = chatModel.call(prompt);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals("Hello from WireMock!", response.getResult().getOutput().getText());

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .withHeader("Authorization", equalTo("Bearer test-api-key")));
    }

    @Test
    @DisplayName("模拟智谱 API 返回 500 错误")
    void testZhipuApiServerError() {
        String baseUrl = "http://localhost:" + wireMock.getPort() + "/api/paas/v4";

        wireMock.stubFor(post(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                    "  \"error\": {\n" +
                    "    \"message\": \"Internal server error\",\n" +
                    "    \"type\": \"server_error\",\n" +
                    "    \"code\": 500\n" +
                    "  }\n" +
                    "}")));

        ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModelWithRealHttp(baseUrl);

        Prompt prompt = new Prompt(new UserMessage("Hello"));

        assertThrows(Exception.class, () -> chatModel.call(prompt));
    }

    @Test
    @DisplayName("模拟智谱 API 返回 401 认证失败")
    void testZhipuApiAuthFailure() {
        String baseUrl = "http://localhost:" + wireMock.getPort() + "/api/paas/v4";

        wireMock.stubFor(post(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                    "  \"error\": {\n" +
                    "    \"message\": \"Invalid API key\",\n" +
                    "    \"type\": \"authentication_error\",\n" +
                    "    \"code\": 401\n" +
                    "  }\n" +
                    "}")));

        ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModelWithRealHttp(baseUrl);

        Prompt prompt = new Prompt(new UserMessage("Hello"));

        assertThrows(Exception.class, () -> chatModel.call(prompt));
    }

    @Test
    @DisplayName("模拟智谱 API 延迟响应")
    void testZhipuApiDelayedResponse() throws InterruptedException {
        String baseUrl = "http://localhost:" + wireMock.getPort() + "/api/paas/v4";

        wireMock.stubFor(post(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withFixedDelay(500)
                .withBody("{\n" +
                    "  \"id\": \"chatcmpl-delayed\",\n" +
                    "  \"choices\": [{\n" +
                    "    \"index\": 0,\n" +
                    "    \"message\": {\n" +
                    "      \"role\": \"assistant\",\n" +
                    "      \"content\": \"Delayed response\"\n" +
                    "    },\n" +
                    "    \"finish_reason\": \"stop\"\n" +
                    "  }]\n" +
                    "}")));

        ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModelWithRealHttp(baseUrl);

        Prompt prompt = new Prompt(new UserMessage("Hello"));

        long startTime = System.currentTimeMillis();
        var response = chatModel.call(prompt);
        long elapsed = System.currentTimeMillis() - startTime;

        assertNotNull(response);
        assertEquals("Delayed response", response.getResult().getOutput().getText());
        assertTrue(elapsed >= 500, "Expected delay of at least 500ms, but was " + elapsed + "ms");
    }

    @Test
    @DisplayName("模拟智谱 API 返回空内容")
    void testZhipuApiEmptyContent() {
        String baseUrl = "http://localhost:" + wireMock.getPort() + "/api/paas/v4";

        wireMock.stubFor(post(urlPathEqualTo("/api/paas/v4/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                    "  \"id\": \"chatcmpl-empty\",\n" +
                    "  \"choices\": [{\n" +
                    "    \"index\": 0,\n" +
                    "    \"message\": {\n" +
                    "      \"role\": \"assistant\",\n" +
                    "      \"content\": \"\"\n" +
                    "    },\n" +
                    "    \"finish_reason\": \"stop\"\n" +
                    "  }]\n" +
                    "}")));

        ZhipuModelAdapter.ZhipuChatModel chatModel = createChatModelWithRealHttp(baseUrl);

        Prompt prompt = new Prompt(new UserMessage("Hello"));
        var response = chatModel.call(prompt);

        assertNotNull(response);
        assertEquals("", response.getResult().getOutput().getText());
    }

    /**
     * 真实 HTTP 客户端实现（使用 Spring RestTemplate）
     */
    private static class RealHttpClient implements HttpClient {
        private final String baseUrl;
        private final org.springframework.web.client.RestTemplate restTemplate;
        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        public RealHttpClient(String baseUrl) {
            this.baseUrl = baseUrl;
            this.restTemplate = new org.springframework.web.client.RestTemplate();
            this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        }

        @Override
        public Object post(String url, java.util.Map<String, Object> headers, Object requestBody) {
            try {
                org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
                headers.forEach((key, value) -> httpHeaders.set(key, value.toString()));

                String jsonBody = objectMapper.writeValueAsString(requestBody);
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, httpHeaders);

                String response = restTemplate.postForObject(url, entity, String.class);
                return objectMapper.readValue(response, java.util.Map.class);
            } catch (Exception e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        }
    }
}
