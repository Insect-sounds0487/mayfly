package io.mayfly.adapter.impl;

import io.mayfly.adapter.http.HttpClient;
import io.mayfly.core.ModelConfig;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

/**
 * 简易性能基准测试
 * 测试适配器性能开销（无需JMH复杂配置）
 */
public class SimpleBenchmark {

    public static void main(String[] args) {
        System.out.println("=== Mayfly Adapter Performance Benchmark ===\n");

        HttpClient mockClient = (url, headers, requestBody) -> {
            return Map.of(
                "id", "chatcmpl-benchmark",
                "choices", List.of(Map.of(
                    "index", 0,
                    "message", Map.of("role", "assistant", "content", "Benchmark response"),
                    "finish_reason", "stop"
                )),
                "usage", Map.of(
                    "prompt_tokens", 10,
                    "completion_tokens", 20,
                    "total_tokens", 30
                )
            );
        };

        ZhipuModelAdapter adapter = new ZhipuModelAdapter() {
            @Override
            protected HttpClient createHttpClient(String apiKey, String baseUrl, String model) {
                return mockClient;
            }
        };

        ZhipuModelAdapter.ZhipuChatModel chatModel = (ZhipuModelAdapter.ZhipuChatModel) adapter.createChatModel(ModelConfig.builder()
            .name("benchmark-model")
            .provider("zhipu")
            .model("glm-4")
            .apiKey("test-key")
            .build());

        Prompt prompt = new Prompt(new UserMessage("Hello benchmark"));

        // Warmup
        System.out.println("Warming up (100 iterations)...");
        for (int i = 0; i < 100; i++) {
            chatModel.call(prompt);
        }

        // Benchmark: Simple prompt
        System.out.println("\nBenchmark: Simple prompt (1000 iterations)");
        int iterations = 1000;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            chatModel.call(prompt);
        }
        long endTime = System.nanoTime();
        long totalMs = (endTime - startTime) / 1_000_000;
        double avgNs = (double) (endTime - startTime) / iterations;
        double avgUs = avgNs / 1000;

        System.out.println("Total time: " + totalMs + " ms");
        System.out.println("Average time: " + String.format("%.2f", avgUs) + " μs/op");
        System.out.println("Throughput: " + String.format("%.0f", iterations * 1000.0 / totalMs) + " ops/sec");

        // Benchmark: Large prompt
        System.out.println("\nBenchmark: Large prompt (1000 iterations)");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is test content line ").append(i).append("\n");
        }
        Prompt largePrompt = new Prompt(new UserMessage(largeContent.toString()));

        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            chatModel.call(largePrompt);
        }
        endTime = System.nanoTime();
        totalMs = (endTime - startTime) / 1_000_000;
        avgNs = (double) (endTime - startTime) / iterations;
        avgUs = avgNs / 1000;

        System.out.println("Total time: " + totalMs + " ms");
        System.out.println("Average time: " + String.format("%.2f", avgUs) + " μs/op");
        System.out.println("Throughput: " + String.format("%.0f", iterations * 1000.0 / totalMs) + " ops/sec");

        System.out.println("\n=== Benchmark Complete ===");
    }
}
