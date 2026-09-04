package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目的【启动类】，整个应用从这里跑起来。
 *
 * 使用 IDEA 时：
 *  1. 右键这个文件 → Run 'RagApplication'
 *  2. 控制台出现 "Started RagApplication" 且 Tomcat started on port 8080
 *     就说明启动成功
 *
 * 一句话理解：启动类 = 一个"总开关"，@SpringBootApplication 告诉 Spring：
 * 从这里开始扫描它所在包（com.example.rag）及其子包下的所有组件。
 */
@SpringBootApplication // 组合注解：自动配置 + 组件扫描 + 开启配置
public class RagApplication {

    /**
     * main 方法：Java 程序的入口，JVM 一执行就先进这里。
     * SpringApplication.run(...) 会启动内嵌的 Tomcat 服务器并加载整个应用。
     *
     * @param args 命令行参数（一般用不到）
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用，传入口类 + 命令行参数
        SpringApplication.run(RagApplication.class, args);
    }
}
