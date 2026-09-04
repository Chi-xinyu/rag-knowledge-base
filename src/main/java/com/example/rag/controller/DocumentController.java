package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.entity.Document;
import com.example.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【文档接口】——第 2 步的核心演示：连 MySQL 查数据 + 走 Redis 缓存。
 *
 * 测试地址（启动项目后浏览器访问）：
 *   1) 查全部：   http://localhost:8080/api/document/list
 *   2) 查单个：   http://localhost:8080/api/document/1
 *
 * 第一次访问：日志会打印【缓存未命中】→ 查数据库 → 写缓存
 * 第二次访问：日志会打印【缓存命中】→ 直接从 Redis 返回
 * 等 60 秒后再访问：缓存过期 → 又变成【缓存未命中】→ 重新查库
 * 这就是完整演示了"缓存命中/未命中/过期"三个状态。
 */
@RestController              // 接口控制器：返回值自动转 JSON
@RequestMapping("/api/document") // 路径前缀
@RequiredArgsConstructor
public class DocumentController {

    /** 注入 Service（业务逻辑），Controller 不直接操作数据库，只调 Service */
    private final DocumentService documentService;

    /**
     * GET /api/document/list —— 查询全部文档
     * @return 统一格式 Result，data 里是文档列表
     */
    @GetMapping("/list")
    public Result<List<Document>> list() {
        return Result.success(documentService.listDocuments());
    }

    /**
     * GET /api/document/{id} —— 按 ID 查询单个文档
     * @param id 路径里的 id，如 /api/document/1
     * @return 文档信息；不存在时 data 为 null
     */
    @GetMapping("/{id}")
    public Result<Document> getById(@PathVariable Long id) {
        return Result.success(documentService.getDocumentById(id));
    }
}
