package com.example.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 【HTTP 客户端配置】——创建 RestClient，自动带上 AI 接口的鉴权头。
 *
 * RestClient 是 Spring Boot 3.2+ 内置的 HTTP 客户端，
 * 用来向硅基流动 API 发请求。这里把它配成"每次请求自动带 Authorization 头"，
 * 这样 AiService 里就不用每处重复写鉴权了。
 */
@Configuration
public class RestClientConfig {

    /**
     * 从配置里取 API Key（application.yml 中 ai.api-key）。
     * 注意：@Value 读取失败会让启动报错，所以 api-key 必须先在配置里填好。
     */
    @Value("${ai.api-key}")
    private String apiKey;

    /**
     * 创建 RestClient Bean：
     *  - defaultHeader：给所有请求默认加上 Authorization: Bearer <key>（鉴权）
     *  - 返回后，AiService 里注入的 restClient 就是这个对象
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
}
