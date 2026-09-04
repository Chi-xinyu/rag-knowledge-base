-- ============================================================
-- 数据库初始化脚本：创建"知识库文档"表 + 插入示例数据
--
-- 使用方法（二选一）：
--  方式 A（推荐）：命令行/IDEA 数据库工具里手动执行
--  方式 B：等程序跑起来后，用后面的 /api/document/init 接口一键执行
--
-- 说明：这是为 RAG 项目准备的"文档"表，第 3 步做知识库问答时
--      存的就是这类文档（标题 + 正文内容）。
-- ============================================================

-- 使用数据库（docker-compose 已自动创建 rag_db，这句保险起见）
USE rag_db;

-- 建表：DROP 先删旧表，CREATE 再建新表（重复执行才不报错）
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    `title`       VARCHAR(255) NOT NULL COMMENT '文档标题',
    `content`     TEXT         NULL COMMENT '文档正文内容',
    `file_type`   VARCHAR(20)  NULL COMMENT '文件类型，如 md/pdf/txt',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未索引 1已索引',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档表';

-- 插入 2 条示例数据，方便一启动就能查到东西
INSERT INTO `document` (`title`, `content`, `file_type`) VALUES
('Java 入门指南', 'Java 是一种面向对象的编程语言，广泛应用于企业级后端开发。', 'md'),
('Spring Boot 快速上手', 'Spring Boot 简化了 Spring 应用的搭建与配置，内置 Tomcat，开箱即用。', 'md');

-- ============================================================
-- 【第 3 步新增】文档切片表：存 RAG 的"文档片段 + 向量"
--
-- 作用：上传一篇文档后，程序会把它切成多段（chunk），
--      每段调用 AI 接口生成一个"向量"存进这个表。
--      提问时，把问题也转成向量，和这里每一段的向量做相似度计算，
--      找出最相关的几段，喂给大模型生成答案。
-- ============================================================
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    `document_id` BIGINT       NOT NULL COMMENT '所属文档ID，关联 document 表',
    `chunk_index` INT          NOT NULL COMMENT '这是文档里的第几段（从0开始）',
    `content`     TEXT         NOT NULL COMMENT '这一段的文字内容',
    `embedding`   TEXT         NULL COMMENT '这一段文字对应的向量（AI 生成的一串数字，存成 JSON）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文档切片（RAG 向量存储）表';
