package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.entity.Conversation;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.entity.Message;
import com.example.rag.mapper.ConversationMapper;
import com.example.rag.mapper.MessageMapper;
import com.example.rag.service.DocumentParserService;
import com.example.rag.service.RagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 【RAG 接口】——第 3 步的核心：知识库问答系统入口。
 *
 * 测试地址（启动项目后）：
 *   1) 上传文档建索引（方式一：直接传文本）：
 *      POST http://localhost:8080/api/rag/document
 *      Body(JSON): {"title":"Spring Boot 简介","content":"Spring Boot 是……"}
 *   2) 上传文档建索引（方式二：上传文件，第 4 步新增）：
 *      POST http://localhost:8080/api/rag/document/upload
 *      Body(form-data): file=xxx.pdf  （支持 PDF/Word/Excel/MD/TXT）
 *   3) 提问（会先去向量库检索，再让大模型基于资料回答）：
 *      GET http://localhost:8080/api/rag/ask?question=什么是Spring Boot
 *   4) 提问（流式版，第 4 步新增：回答逐字返回，打字机效果）：
 *      GET http://localhost:8080/api/rag/ask/stream?question=什么是Spring Boot
 *
 * 完整演示流程：
 *   先 POST 几篇文档 → 再 GET ask 提问 → 看到回答下方带着"参考的知识片段"
 *   说明：程序真的先检索到了相关文档，再生成的答案（这就是 RAG 的价值）。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagService ragService;
    private final DocumentParserService documentParserService;
    private final ObjectMapper objectMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    /** 流式推送用的线程池：大模型生成是耗时操作，不能占着 Tomcat 的请求线程 */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 把一条消息（用户问 / AI 答）保存到会话里。
     * 第 5 步新增：支持多轮对话 + 会话记录。
     */
    private void saveMessage(Long conversationId, String role, String content) {
        if (conversationId == null) return; // 没选会话就不保存（兼容旧用法）
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        messageMapper.insert(m);
        // 顺手更新会话的更新时间（让"最近聊过的排前面"生效）
        Conversation c = conversationMapper.selectById(conversationId);
        if (c != null) {
            c.setTitle(c.getTitle() == null || c.getTitle().isBlank() || "新会话".equals(c.getTitle())
                    ? (content.length() > 30 ? content.substring(0, 30) + "…" : content)
                    : c.getTitle());
            conversationMapper.updateById(c);
        }
    }

    /**
     * POST /api/rag/document —— 上传一篇文档（纯文本方式），自动切片+向量化建索引
     * 请求体示例：{"title":"xxx","content":"yyy"}
     */
    @PostMapping("/document")
    public Result<Document> addDocument(@RequestBody Map<String, String> req) {
        String title = req.getOrDefault("title", "未命名文档");
        String content = req.getOrDefault("content", "");
        return Result.success(ragService.addDocument(title, content, "txt"));
    }

    /**
     * POST /api/rag/document/upload —— 上传文件（第 4 步新增）
     * 支持 PDF / Word(.docx) / Excel(.xlsx) / Markdown / TXT
     * 后端自动解析成纯文本 → 切片 → 向量化 → 建索引
     */
    @PostMapping("/document/upload")
    public Result<Document> uploadDocument(@RequestParam("file") MultipartFile file) {
        // 1. 解析文件 → 纯文本
        String content = documentParserService.parse(file);

        // 2. 用文件名（去掉后缀）当标题，解析出的文本当内容，入库建索引
        String name = file.getOriginalFilename();
        String title = name != null && name.contains(".")
                ? name.substring(0, name.lastIndexOf('.'))
                : (name != null ? name : "未命名文档");

        // 3. 提取真实文件类型（pdf/docx/xlsx/md/txt）
        String type = name != null && name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase()
                : "txt";

        return Result.success(ragService.addDocument(title, content, type));
    }

    /**
     * GET /api/rag/ask —— 提问（RAG 问答）
     * 例：/api/rag/ask?question=什么是Spring Boot
     */
    @GetMapping("/ask")
    public Result<String> ask(@RequestParam("question") String question) {
        return Result.success(ragService.ask(question));
    }

    /**
     * GET /api/rag/ask/stream —— 提问（流式版，第 4 步新增）
     * 例：/api/rag/ask/stream?question=什么是Spring Boot
     *
     * 返回格式：SSE（Server-Sent Events，服务器推送事件）
     * 浏览器会"收到一个字显示一个字"，实现打字机效果。
     *
     * 前端用 fetch 读取，事件格式为：
     *   data: {"type":"delta","content":"你"}   每生成一段推一次
     *   data: {"type":"refs","content":[...]}   参考片段
     *   data: {"type":"done"}                   结束
     *   data: {"type":"error","content":"..."}  出错
     */
    @GetMapping(value = "/ask/stream", produces = "text/event-stream")
    public SseEmitter askStream(@RequestParam("question") String question,
                                @RequestParam(value = "conversationId", required = false) Long conversationId) {
        // 1. 创建 SSE 发射器：往浏览器推事件的通道（60 秒超时）
        SseEmitter emitter = new SseEmitter(60_000L);

        // 2. 如果有会话，先把用户的问题存进去（第 5 步新增）
        saveMessage(conversationId, "user", question);

        // 3. 大模型生成较慢，放到线程池异步执行，不占请求线程
        sseExecutor.execute(() -> {
            // 累积完整答案：流式推送结束后，把完整回答也存进会话
            StringBuilder fullAnswer = new StringBuilder();
            try {
                // 4. 检索 + 流式生成：每生成一段文字，就包装成 JSON 推给前端
                List<DocumentChunk> chunks = ragService.askStream(question, delta -> {
                    fullAnswer.append(delta);
                    try {
                        String json = objectMapper.writeValueAsString(
                                Map.of("type", "delta", "content", delta));
                        emitter.send(SseEmitter.event().data(json));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                // 5. 推参考片段（type=refs）
                String refsJson = objectMapper.writeValueAsString(
                        Map.of("type", "refs",
                                "content", chunks.stream().map(DocumentChunk::getContent).toList()));
                emitter.send(SseEmitter.event().data(refsJson));

                // 6. 保存 AI 的回答到会话（第 5 步新增）
                saveMessage(conversationId, "assistant", fullAnswer.toString());

                // 7. 推结束标记，正常收尾
                emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流式问答失败", e);
                // 出错也要通知前端，否则前端会一直等。
                // ⚠️ 必须用 objectMapper 转义！直接拼字符串的话，
                //    错误信息里的双引号/换行会弄坏 JSON，前端解析就崩了。
                try {
                    String errorJson = objectMapper.writeValueAsString(
                            Map.of("type", "error", "content", e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorJson));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
