package com.campus.common.core.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * <p>前端约定：code=200 表示成功，其余为业务/系统错误码。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "统一响应体")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    @Schema(description = "业务状态码，200 成功")
    private int code;

    /** 提示信息 */
    @Schema(description = "提示信息")
    private String msg;

    /** 业务数据 */
    @Schema(description = "业务数据")
    private T data;

    /** 服务端时间戳 */
    @Schema(description = "服务端时间戳")
    private long timestamp = System.currentTimeMillis();

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> ok(String msg, T data) {
        Result<T> r = ok(data);
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
