package com.example.rag.service;

import com.example.rag.config.AiProperties;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 【RAG 服务】——把整条"检索增强生成"链路串起来，对外提供两个能力。
 *
 * RAG 全流程（面试必背，就这 4 步）：
 *   索引阶段：
 *     1. 文档切片：把长文档切成小段（VectorStoreService.indexDocument 里完成）
 *     2. 向量化存储：每段文字 → 向量 → 存 MySQL
 *   问答阶段：
 *     3. 语义检索：问题 → 向量 → 找到最相关的几段（search）
 *     4. 增强生成：把"问题 + 检索到的资料"一起发给大模型，生成答案（AiService.chat）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final DocumentMapper documentMapper;
    private final VectorStoreService vectorStoreService;
    private final AiService aiService;
    private final AiProperties aiProperties;

    /**
     * 【建索引】上传一篇文档（title + content），
     * 保存到 document 表，然后切片+向量化存进向量表。
     * @return 保存后的文档（带 ID）
     */
    public Document addDocument(String title, String content) {
        // 1. 先存文档基本信息
        Document doc = new Document();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setFileType("txt");
        doc.setStatus(0); // 0=未索引（随后置 1）
        documentMapper.insert(doc);

        // 2. 切片 + 向量化 + 存向量库
        vectorStoreService.indexDocument(doc);

        // 3. 标记为"已索引"
        doc.setStatus(1);
        documentMapper.updateById(doc);
        return doc;
    }

    /**
     * 【问答】核心入口：用户提问 → 检索相关资料 → 生成答案。
     * @param question 用户问题
     * @return 大模型生成的回答（含引用的资料来源）
     */
    public String ask(String question) {
        // ---- 第 3 步：语义检索，找到最相关的几段资料 ----
        List<DocumentChunk> topChunks = vectorStoreService.search(question, aiProperties.getTopK());
        if (topChunks.isEmpty()) {
            return "知识库中还没有内容，请先上传文档。";
        }

        // ---- 第 4 步：把资料拼成上下文，连同问题发给大模型 ----
        String context = topChunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.joining("\n\n"));

        String answer = aiService.chat(question, context);

        // 组装回答 + 引用的资料，方便验证检索是否正确
        StringBuilder sb = new StringBuilder(answer);
        sb.append("\n\n--- 参考的知识片段 ---\n");
        for (DocumentChunk c : topChunks) {
            sb.append("· ").append(c.getContent()).append("\n");
        }
        return sb.toString();
    }
}
