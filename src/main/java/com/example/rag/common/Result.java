package com.example.rag.common;

import lombok.Data;

/**
 * 【统一返回结果类】——让所有接口返回统一格式的 JSON。
 *
 * 为什么要统一？后端接口如果每个都返回不同的结构，前端就不知道怎么解析。
 * 所以约定一个通用格式：
 *   { "code": 200, "message": "ok", "data": { ...实际数据... } }
 *
 * 这是后端开发的基本功，面试常问"你的接口怎么设计统一返回"。
 */
@Data
public class Result<T> {

    /** 状态码：200 = 成功，500 = 失败（约定俗成） */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 真正的业务数据 */
    private T data;

    /** 私有构造，外部只能通过下面的静态方法创建，保证格式统一 */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功：带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "ok", data);
    }

    /** 成功：不带数据 */
    public static <T> Result<T> success() {
        return new Result<>(200, "ok", null);
    }

    /** 失败：带错误信息 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
