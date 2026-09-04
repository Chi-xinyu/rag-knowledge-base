package com.example.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 【AI 配置类】——读取 application.yml 里 ai: 开头的配置。
 *
 * 作用：把配置文件里写的 base-url、api-key、模型名等，
 *      自动绑定到这个类的属性上。代码里别处要用时，
 *      注入这个类就能拿到配置值，不用到处硬编码。
 *
 * 例如 application.yml 里写了：
 *   ai:
 *     base-url: https://api.siliconflow.cn/v1
 *  就会自动映射到本类的 baseUrl 属性。
 */
@Data
@Configuration                // 标记为配置类，Spring 启动时会加载它
@ConfigurationProperties(prefix = "ai") // 绑定配置文件里 "ai:" 前缀下的所有字段
public class AiProperties {

    /** API 基础地址，例如 https://api.siliconflow.cn/v1 */
    private String baseUrl;

    /** API 密钥（在硅基流动平台申请） */
    private String apiKey;

    /** 向量化模型名，例如 BAAI/bge-m3 */
    private String embeddingModel;

    /** 对话（生成答案）模型名，例如 deepseek-ai/DeepSeek-V3 */
    private String chatModel;

    /** 检索时取最相关的文档片段数 */
    private Integer topK = 3;

    /** 向量维度，bge-m3 为 1024 */
    private Integer embeddingDim = 1024;
}
