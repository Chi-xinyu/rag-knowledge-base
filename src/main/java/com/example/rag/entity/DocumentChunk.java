package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【实体类】——对应数据库里的 document_chunk 表。
 *
 * 这是 RAG 的核心存储单元：
 * 一篇长文档会被切分成很多"片段(chunk)"，每段一行存这个表，
 * 每段文字 + 它对应的向量(embedding) 一起存下来。
 * 提问时就在这些向量里找最相关的一段。
 */
@Data
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文档 ID（关联 document 表） */
    private Long documentId;

    /** 这是该文档的第几段（从 0 开始） */
    private Integer chunkIndex;

    /** 这一段的文字内容 */
    private String content;

    /**
     * 这一段的向量（一串浮点数）。
     * 为了简单，我们存成 JSON 字符串，例如 "[0.12, -0.34, 0.56...]"
     * 真实项目会用专业的向量数据库（Milvus 等），我们先用 MySQL 存储教学。
     */
    private String embedding;

    private LocalDateTime createTime;
}
