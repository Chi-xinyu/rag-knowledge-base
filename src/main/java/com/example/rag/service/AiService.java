package com.example.rag.service;

import com.example.rag.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【AI 服务类】——封装对"大模型 API"的两个核心调用。
 *
 * 1. textToEmbedding(text) ：把一段文字转成向量（一串浮点数），RAG 的"索引"和"检索"都靠它
 * 2. chat(question, context)：把问题和检索到的资料一起发给大模型，让它"看着资料回答"
 *
 * 原理：硅基流动提供 OpenAI 兼容接口，所以按 OpenAI 的标准格式发 HTTP 请求即可。
 * 用 Spring Boot 自带的 RestClient 发请求，不需要额外引第三方库。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    /** AI 配置（api-key、模型名等） */
    private final AiProperties aiProperties;

    /** Jackson JSON 工具：拼请求、解析响应 */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端（Spring Boot 内置，发 POST 请求到 AI 接口） */
    private final RestClient restClient;

    /**
     * 【向量化】把一段文字转成向量。
     *
     * @param text 要向量化的文字
     * @return 一串浮点数（例如 1024 个），相同含义的文字，向量越"接近"
     */
    public double[] textToEmbedding(String text) {
        // 1. 构造请求体：OpenAI /embeddings 接口的标准格式
        //    model: 用哪个向量化模型；input: 要转换的文字
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getEmbeddingModel());
        body.put("input", text);

        // 2. 发 POST 请求到 {baseUrl}/embeddings，拿到 JSON 响应
        JsonNode resp = restClient.post()
                .uri(aiProperties.getBaseUrl() + "/embeddings")
                .contentType(MediaType.APPLICATION_JSON) // 告诉服务器"我发的是 JSON"
                .body(body)
                .retrieve()
                .body(JsonNode.class); // 把响应解析成 JSON 树

        // 3. 从响应里取向量：标准格式是 data[0].embedding = [0.123, -0.456, ...]
        JsonNode embeddingNode = resp.path("data").path(0).path("embedding");
        double[] vector = new double[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = embeddingNode.get(i).asDouble();
        }
        return vector;
    }

    /**
     * 【对话生成】把问题 + 检索到的资料一起发给大模型，生成基于资料的答案。
     *
     * @param question 用户的问题
     * @param context  检索到的相关资料片段（拼接成一段文字）
     * @return 大模型生成的回答文本
     */
    public String chat(String question, String context) {
        // 1. 构造消息列表：system 设定角色，user 放"资料 + 问题"
        List<Map<String, String>> messages = new ArrayList<>();

        // system 消息：告诉模型"你的行为准则"——只依据给的资料回答，避免它瞎编
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", "你是一个知识库问答助手。请严格根据下面提供的【参考资料】回答用户问题。"
                + "如果资料中没有相关信息，请明确回答'资料中没有找到相关内容'，不要编造。");
        messages.add(system);

        // user 消息：把检索到的资料和问题一起发给模型
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", "【参考资料】\n" + context + "\n\n【用户问题】\n" + question);
        messages.add(user);

        // 2. 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getChatModel());
        body.put("messages", messages);
        body.put("temperature", 0.3); // 温度越低，回答越保守稳定

        // 3. 发请求到 {baseUrl}/chat/completions
        JsonNode resp = restClient.post()
                .uri(aiProperties.getBaseUrl() + "/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        // 4. 取答案：标准格式是 choices[0].message.content
        return resp.path("choices").path(0).path("message").path("content").asText();
    }
}
