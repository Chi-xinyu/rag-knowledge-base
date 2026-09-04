package com.example.rag.service.impl;

import com.example.rag.entity.Document;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.DocumentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 【Service 实现类】——业务逻辑真正写在这里。
 *
 * 核心演示点：Redis 缓存（经典套路，面试必问）
 *   套路 = "先查缓存，缓存有就直接返回（快）；缓存没有就去查数据库，
 *          查完把结果【序列化成 JSON】写进缓存并设过期时间，
 *          下次请求先读缓存，命中就反序列化返回，挡掉重复的数据库查询"
 *   好处：高并发场景下挡住大量重复请求，减轻数据库压力。
 *
 * 为什么用 StringRedisTemplate + 手动 JSON 序列化？
 *   这种方式最简单直观：Redis 里存的都是字符串（JSON），
 *   你能直接在 Redis 客户端里看到内容，最适合新手学习和排查。
 */
@Service                       // 标记为 Spring 的 Service 组件，交给 Spring 管理
@RequiredArgsConstructor       // Lombok：自动生成"用 final 字段"的构造方法（依赖注入）
@Slf4j                        // Lombok：提供 log 日志对象，用 log.info(...) 打日志
public class DocumentServiceImpl implements DocumentService {

    /** 数据库操作：Spring 自动注入（因为是 final 字段 + 构造注入） */
    private final DocumentMapper documentMapper;

    /** Redis 操作：Spring 自动注入。StringRedisTemplate 以"字符串"形式读写，最简单 */
    private final StringRedisTemplate redisTemplate;

    /** Jackson 的 JSON 工具：负责"对象 <-> JSON 字符串"互转 */
    private final ObjectMapper objectMapper;

    /** Redis 缓存过期时间（秒）：60 秒后自动失效，模拟"热点数据短暂缓存" */
    private static final long CACHE_TTL_SECONDS = 60;

    /** 缓存 key：列表用一个固定 key；单条用 "前缀 + id"，这样每条一个 key */
    private static final String CACHE_KEY_LIST = "document:list";
    private static final String CACHE_KEY_PREFIX_ID = "document:id:";

    @Override
    public List<Document> listDocuments() {
        // ---------- 第 1 步：先查缓存 ----------
        String cacheValue = redisTemplate.opsForValue().get(CACHE_KEY_LIST);
        if (cacheValue != null) {
            // 缓存命中：把 JSON 字符串反序列化回 List<Document> 返回
            log.info("【缓存命中】文档列表来自 Redis");
            try {
                return objectMapper.readValue(cacheValue, new TypeReference<List<Document>>() {});
            } catch (Exception e) {
                log.warn("缓存反序列化失败，忽略缓存，回查数据库", e);
            }
        }

        // ---------- 第 2 步：缓存没有（或反序列化失败）→ 查数据库 ----------
        log.info("【缓存未命中】查询数据库 document 表");
        List<Document> list = documentMapper.selectList(null); // null = 无条件查全部

        // ---------- 第 3 步：把结果写进缓存，设过期时间 ----------
        try {
            String json = objectMapper.writeValueAsString(list); // 对象 -> JSON
            redisTemplate.opsForValue().set(CACHE_KEY_LIST, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 序列化失败不影响主流程，记个日志即可（缓存是"锦上添花"，数据库才是"真相"）
            log.warn("文档列表写入缓存失败", e);
        }
        return list;
    }

    @Override
    public Document getDocumentById(Long id) {
        // 单条数据缓存：key 用 "前缀 + id"，每条数据一个独立缓存
        String cacheKey = CACHE_KEY_PREFIX_ID + id;

        // ---------- 第 1 步：先查缓存 ----------
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            log.info("【缓存命中】文档 id={} 来自 Redis", id);
            try {
                return objectMapper.readValue(cacheValue, Document.class);
            } catch (Exception e) {
                log.warn("缓存反序列化失败，忽略缓存，回查数据库", e);
            }
        }

        // ---------- 第 2 步：查数据库 ----------
        log.info("【缓存未命中】从数据库查询文档 id={}", id);
        Document doc = documentMapper.selectById(id); // 按主键查单条

        // ---------- 第 3 步：写缓存 ----------
        if (doc != null) {
            try {
                String json = objectMapper.writeValueAsString(doc);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("文档写入缓存失败", e);
            }
        }
        return doc;
    }
}
