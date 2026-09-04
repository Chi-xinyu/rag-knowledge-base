package com.example.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 【Mapper 接口】——document_chunk 表的数据库操作入口。
 * 继承 BaseMapper 就自动获得增删改查能力，和 DocumentMapper 用法一样。
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
    // 需要自定义 SQL（比如"按向量相似度排序"）时再加方法，本步先用基础方法
}
