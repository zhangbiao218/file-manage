package com.tiansuo.file.manage.response;

import lombok.Getter;

import java.util.Arrays;


@Getter
public enum ReturnCodeEnum {
    /**操作成功**/
    RC200(200,"success"),
    RC404(404,"404页面找不到的异常"),
    /**服务异常**/
    RC500(500,"系统异常，请稍后重试");

    private final Integer code; // 自定义的状态码
    private final String message; // 自定义的描述信息

    ReturnCodeEnum(Integer code, String message)
    {
        this.code = code;
        this.message = message;
    }

    public static ReturnCodeEnum getReturnCodeEnum(Integer code)
    {
        return Arrays.stream(ReturnCodeEnum.values()).filter(x -> x.getCode().equals(code)).findFirst().orElse(null);
    }

}
