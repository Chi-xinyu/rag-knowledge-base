package com.example.rag.service;

import com.example.rag.entity.Document;

import java.util.List;

/**
 * 【Service 接口】——业务逻辑层。
 *
 * 分层思想（后端面试必问）：
 *  Controller（表现层）  → 接收请求、返回结果
 *  Service（业务层）     → 写具体业务逻辑（本类）
 *  Mapper（数据层）      → 操作数据库
 *
 * 好处：业务逻辑和接口/数据库解耦，方便复用、测试、维护。
 * 这里定义"有哪些业务能力"，具体实现写在 DocumentServiceImpl 里。
 */
public interface DocumentService {

    /**
     * 查询全部文档（带 Redis 缓存）
     * @return 文档列表
     */
    List<Document> listDocuments();

    /**
     * 根据 ID 查询单个文档（带 Redis 缓存）
     * @param id 文档主键
     * @return 文档，不存在返回 null
     */
    Document getDocumentById(Long id);
}
