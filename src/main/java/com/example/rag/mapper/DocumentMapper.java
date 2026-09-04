package com.example.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 【Mapper 接口】——数据库操作入口。
 *
 * 这是 MyBatis-Plus 的核心用法：
 * 只要继承 BaseMapper<Document>，就自动获得 insert/deleteById/selectById/
 * selectList 等常用数据库方法，不用自己写 SQL。
 *
 * 比如查全部：documentMapper.selectList(null)  // null 表示无条件
 *     查单条：documentMapper.selectById(1L)
 */
@Mapper // 标记这是一个 MyBatis 的 Mapper，Spring 启动时会扫描并注册它
public interface DocumentMapper extends BaseMapper<Document> {
    // 接口里可以先什么都不写，继承的方法就够用了。
    // 后面需要"自定义复杂查询"时，再在这里加方法 + 写 SQL。
}
