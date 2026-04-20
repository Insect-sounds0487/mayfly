package io.mayfly.adapter.http;

import java.util.Map;

/**
 * HTTP 客户端接口
 * 用于支持 Mock 测试和不同实现切换
 */
public interface HttpClient {
    
    /**
     * 执行 POST 请求
     * @param url 请求 URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 响应数据
     */
    Object post(String url, Map<String, Object> headers, Object requestBody);
}
