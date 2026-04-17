package io.mayfly.test;

import io.mayfly.adapter.impl.*;
import io.mayfly.core.ModelConfig;

/**
 * Adapter validation test class
 * Validates basic functionality of all model adapters
 */
public class AdapterValidationTest {

    public static void main(String[] args) {
        System.out.println("Starting validation of all model adapters...");

        // Test Claude adapter
        try {
            ClaudeModelAdapter claudeAdapter = new ClaudeModelAdapter();
            System.out.println("✓ ClaudeModelAdapter created successfully, provider: " + claudeAdapter.getProvider());

            // Verify basic functionality
            String provider = claudeAdapter.getProvider();
            assert "claude".equals(provider) : "Claude provider name mismatch";
            System.out.println("✓ ClaudeModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ ClaudeModelAdapter validation failed: " + e.getMessage());
        }

        // Test Zhipu adapter
        try {
            ZhipuModelAdapter zhipuAdapter = new ZhipuModelAdapter();
            System.out.println("✓ ZhipuModelAdapter created successfully, provider: " + zhipuAdapter.getProvider());

            String provider = zhipuAdapter.getProvider();
            assert "zhipu".equals(provider) : "Zhipu provider name mismatch";
            System.out.println("✓ ZhipuModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ ZhipuModelAdapter validation failed: " + e.getMessage());
        }

        // Test DeepSeek adapter
        try {
            DeepSeekModelAdapter deepSeekAdapter = new DeepSeekModelAdapter();
            System.out.println("✓ DeepSeekModelAdapter created successfully, provider: " + deepSeekAdapter.getProvider());

            String provider = deepSeekAdapter.getProvider();
            assert "deepseek".equals(provider) : "DeepSeek provider name mismatch";
            System.out.println("✓ DeepSeekModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ DeepSeekModelAdapter validation failed: " + e.getMessage());
        }

        // Test Tongyi adapter
        try {
            TongyiModelAdapter tongyiAdapter = new TongyiModelAdapter();
            System.out.println("✓ TongyiModelAdapter created successfully, provider: " + tongyiAdapter.getProvider());

            String provider = tongyiAdapter.getProvider();
            assert "tongyi".equals(provider) : "Tongyi provider name mismatch";
            System.out.println("✓ TongyiModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ TongyiModelAdapter validation failed: " + e.getMessage());
        }

        // Test OpenAI adapter
        try {
            OpenAiModelAdapter openaiAdapter = new OpenAiModelAdapter();
            System.out.println("✓ OpenAiModelAdapter created successfully, provider: " + openaiAdapter.getProvider());

            String provider = openaiAdapter.getProvider();
            assert "openai".equals(provider) : "OpenAI provider name mismatch";
            System.out.println("✓ OpenAiModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ OpenAiModelAdapter validation failed: " + e.getMessage());
        }

        // Test Wenxin adapter
        try {
            WenxinModelAdapter wenxinAdapter = new WenxinModelAdapter();
            System.out.println("✓ WenxinModelAdapter created successfully, provider: " + wenxinAdapter.getProvider());

            String provider = wenxinAdapter.getProvider();
            assert "wenxin".equals(provider) : "Wenxin provider name mismatch";
            System.out.println("✓ WenxinModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ WenxinModelAdapter validation failed: " + e.getMessage());
        }

        // Test Xinghuo adapter
        try {
            XinghuoModelAdapter xinghuoAdapter = new XinghuoModelAdapter();
            System.out.println("✓ XinghuoModelAdapter created successfully, provider: " + xinghuoAdapter.getProvider());

            String provider = xinghuoAdapter.getProvider();
            assert "xinghuo".equals(provider) : "Xinghuo provider name mismatch";
            System.out.println("✓ XinghuoModelAdapter function validation passed");
        } catch (Exception e) {
            System.out.println("✗ XinghuoModelAdapter validation failed: " + e.getMessage());
        }

        System.out.println("\nAll adapter validations completed!");
        System.out.println("Supported model providers:");
        System.out.println("- Claude (Anthropic)");
        System.out.println("- Zhipu (ZhipuAI)");
        System.out.println("- DeepSeek");
        System.out.println("- Tongyi (Tongyi Qianwen)");
        System.out.println("- OpenAI");
        System.out.println("- Wenxin (Wenxin Yiyan)");
        System.out.println("- Xinghuo (Xunfei Xinghuo)");
    }
}