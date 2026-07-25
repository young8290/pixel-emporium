package com.example.common.response;

import com.example.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Schema(description = "通用返回值")
public class R<T> implements Serializable {
    /*
    类似http状态码的作用
     */
    @Schema(description = "业务状态码")
    private Integer code;

    /*
    额外消息（如错误消息）
     */
    @Schema(description = "额外消息（如错误消息）")
    private String msg;

    /*
    具体的数据（不知道什么类型，所以用泛型）
     */
    @Schema(description = "具体的数据")
    private T data;

    public R() {
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "", data);
    }

    public static <T> R<T> ok() {
        return new R<>(200, "", null);
    }

    public static <T> R<T> fail(Integer code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(errorCode.getCode(), msg, null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(HttpStatus.BAD_REQUEST.value(), msg, null);
    }

    public static <T> R<T> failValid(String msg) {
        return new R<>(HttpStatus.UNPROCESSABLE_ENTITY.value(), msg != null ? msg : HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
