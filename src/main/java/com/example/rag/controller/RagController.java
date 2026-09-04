package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.entity.Document;
import com.example.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 【RAG 接口】——第 3 步的核心：知识库问答系统入口。
 *
 * 测试地址（启动项目后）：
 *   1) 上传文档建索引：
 *      POST http://localhost:8080/api/rag/document
 *      Body(JSON): {"title":"Spring Boot 简介","content":"Spring Boot 是……"}
 *   2) 提问（会先去向量库检索，再让大模型基于资料回答）：
 *      GET http://localhost:8080/api/rag/ask?question=什么是Spring Boot
 *
 * 完整演示流程：
 *   先 POST 几篇文档 → 再 GET ask 提问 → 看到回答下方带着"参考的知识片段"
 *   说明：程序真的先检索到了相关文档，再生成的答案（这就是 RAG 的价值）。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * POST /api/rag/document —— 上传一篇文档，自动切片+向量化建索引
     * 请求体示例：{"title":"xxx","content":"yyy"}
     */
    @PostMapping("/document")
    public Result<Document> addDocument(@RequestBody Map<String, String> req) {
        String title = req.getOrDefault("title", "未命名文档");
        String content = req.getOrDefault("content", "");
        return Result.success(ragService.addDocument(title, content));
    }

    /**
     * GET /api/rag/ask —— 提问（RAG 问答）
     * 例：/api/rag/ask?question=什么是Spring Boot
     */
    @GetMapping("/ask")
    public Result<String> ask(@RequestParam("question") String question) {
        return Result.success(ragService.ask(question));
    }
}
