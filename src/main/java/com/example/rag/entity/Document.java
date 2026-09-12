package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 【实体类】——对应数据库里的 document 表。
 *
 * 实体类的作用：把数据库里的一张表，映射成 Java 里的一个类。
 * 一行数据 = 一个对象，一个字段 = 一个属性。MyBatis-Plus 靠它自动做映射。
 */
@Data // Lombok 注解：自动生成 getter/setter/toString 等样板代码，省得手写
@TableName("document") // 告诉 MyBatis-Plus：这个类对应数据库哪张表
public class Document {

    /**
     * 主键：对应表里的 id 字段。
     * @TableId 声明它是主键；IdType.AUTO 表示主键由数据库自增生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题（对应 title 列） */
    private String title;

    /** 文档正文内容（对应 content 列） */
    private String content;

    /** 文件类型 md/pdf/txt（对应 file_type 列） */
    private String fileType;

    /** 状态：0未索引 1已索引（对应 status 列） */
    private Integer status;

    /** 所属知识库 ID（管理员端第 6 步新增，可空=未归类） */
    private Long knowledgeBaseId;

    /** 创建时间（对应 create_time 列） */
    private LocalDateTime createTime;

    /** 更新时间（对应 update_time 列） */
    private LocalDateTime updateTime;
}
