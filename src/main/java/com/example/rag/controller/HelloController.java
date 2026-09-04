package com.example.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【第一个接口】——Hello World 控制器。
 *
 * 作用：验证"项目能不能跑起来 + 请求能不能通"。
 * 启动后浏览器访问  http://localhost:8080/api/hello?name=张三
 * 会返回： 你好，张三！欢迎来到 RAG 项目。
 *
 * 分层小提示：这是【表现层(Controller)】，
 * 后面我们会加 Service（业务逻辑层）、Mapper（数据访问层）来放真正的业务。
 */
@RestController         // 表示这是一个"返回数据的接口控制器"（自动把返回值转成 JSON）
@RequestMapping("/api") // 路径前缀：这个类下所有接口都带 /api 前缀
public class HelloController {

    /**
     * GET 请求接口，完整路径 = 类前缀 /api + 方法路径 /hello = /api/hello
     *
     * @param name 请求参数，通过 @RequestParam 从 URL 中 ?name=xxx 读取
     *             带 defaultValue 表示：不传时给默认值"同学"
     * @return 直接返回字符串，Spring 会自动输出为纯文本
     */
    @GetMapping("/hello")          // 映射 GET /api/hello 到这个方法
    public String hello(
            @RequestParam(value = "name", defaultValue = "同学") String name) {
        // 返回一句话：注意字符串拼接 + 换行符 \n 在网页里会显示为换行
        return "你好，" + name + "！欢迎来到 RAG 项目。";
    }
}
