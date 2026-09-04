package com.example.rag.service;

import com.example.rag.config.AiProperties;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentChunkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 【向量库服务】——RAG 的"存储与检索"核心。
 *
 * 两大职责：
 *  1. indexDocument(doc) ：上传文档时调用。把文档切片 → 每段转向量 → 存 MySQL
 *  2. search(question, topK)：提问时调用。把问题转向量 → 和库里的每段做相似度计算
 *                            → 返回最相关的几段
 *
 * 相似度算法：余弦相似度（cosine similarity），面试必问，原理：
 *  把两个向量看成空间里的两个箭头，箭头方向越一致，说明语义越接近，
 *  计算 = 两向量点积 ÷ 各自长度之积，结果范围 [-1, 1]，越接近 1 越相似。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final DocumentChunkMapper chunkMapper;
    private final AiService aiService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** 每个片段最多多少个字（切片大小，可根据文档类型调整） */
    private static final int CHUNK_SIZE = 200;

    /** 每个片段重叠多少个字（让切片边界处的语义不丢失） */
    private static final int CHUNK_OVERLAP = 50;

    /**
     * 【建索引】把一篇文档切片并向量化，存进数据库。
     * @param doc 已保存到 document 表的文档
     */
    public void indexDocument(Document doc) {
        // 1. 把整篇文档切成小段
        List<String> chunks = splitIntoChunks(doc.getContent());
        log.info("文档《{}》被切成 {} 段，开始向量化...", doc.getTitle(), chunks.size());

        // 2. 每段：转向量 → 存进 document_chunk 表
        for (int i = 0; i < chunks.size(); i++) {
            String text = chunks.get(i);

            // 调用 AI 接口把这段文字转成向量
            double[] vector = aiService.textToEmbedding(text);
            // 把 double[] 转成 JSON 字符串存库，例如 "[0.12,-0.34,...]"
            String vectorJson = toJson(vector);

            // 组装实体并插入数据库
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(text);
            chunk.setEmbedding(vectorJson);
            chunkMapper.insert(chunk);
        }
        log.info("文档《{}》索引完成，共入库 {} 段", doc.getTitle(), chunks.size());
    }

    /**
     * 【语义检索】给定问题，返回最相关的 topK 个文档片段。
     * @param question 用户问题
     * @param topK 要返回几个片段
     * @return 最相关的片段列表（已按相似度从高到低排序）
     */
    public List<DocumentChunk> search(String question, int topK) {
        // 1. 把问题转成向量（和建索引时用同一个模型，向量空间才一致）
        double[] questionVector = aiService.textToEmbedding(question);

        // 2. 取出库里所有片段
        List<DocumentChunk> all = chunkMapper.selectList(null);

        // 3. 计算问题向量和每段的余弦相似度，记录得分
        List<Object[]> scored = new ArrayList<>(); // [片段, 相似度分数]
        for (DocumentChunk chunk : all) {
            double[] chunkVector = fromJson(chunk.getEmbedding());
            double score = cosineSimilarity(questionVector, chunkVector);
            scored.add(new Object[]{chunk, score});
        }

        // 4. 按相似度从高到低排序，取前 topK 个
        scored.sort(Comparator.comparingDouble(o -> -(Double) o[1]));

        List<DocumentChunk> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            result.add((DocumentChunk) scored.get(i)[0]);
        }
        return result;
    }

    /**
     * 余弦相似度：衡量两个向量的"方向接近程度"。
     * 公式：cos = (a·b) / (|a|·|b|)，a·b 是点积，|a| 是向量长度。
     */
    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];      // 累加点积
            normA += a[i] * a[i];    // 累加 a 各分量平方
            normB += b[i] * b[i];    // 累加 b 各分量平方
        }
        // 防止除以 0（向量全为 0 时）
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 把长文本切成小段。策略：
     *  - 优先按中文标点(。！？；)切句，一句一句凑，凑够 CHUNK_SIZE 就成一段
     *  - 段与段之间留 CHUNK_OVERLAP 字的重叠，避免切在语义边界上丢信息
     * 这是 RAG 里"文本切分"的经典实现，面试可以展开讲。
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) return chunks;

        // 按中文标点切成句子（保留标点）
        String[] sentences = content.split("(?<=[。！？；.!?])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            // 当前段加上这句会超过上限，就把当前段收尾，开始新的一段
            if (current.length() > 0 && current.length() + sentence.length() > CHUNK_SIZE) {
                chunks.add(current.toString().trim());
                // 新一段带上上一段末尾的 overlap 个字，保证语义连续
                String tail = current.length() > CHUNK_OVERLAP
                        ? current.substring(current.length() - CHUNK_OVERLAP) : "";
                current = new StringBuilder(tail);
            }
            current.append(sentence);
        }
        // 收尾：最后一段没超上限也要存进去
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    /** double[] -> JSON 字符串 */
    private String toJson(double[] arr) {
        try {
            return objectMapper.writeValueAsString(arr);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
    }

    /** JSON 字符串 -> double[] */
    private double[] fromJson(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            throw new RuntimeException("向量反序列化失败", e);
        }
    }
}
