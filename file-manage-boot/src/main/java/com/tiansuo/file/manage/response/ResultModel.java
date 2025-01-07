package com.tiansuo.file.manage.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;



@Data
@Accessors(chain = true)
public class ResultModel<T>  {
    private Integer code;
    private String message;
    private T data;
    private long timestamp;

    public ResultModel(){
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResultModel<T> success(T data){
        ResultModel res = new ResultModel();
        res.setCode(ReturnCodeEnum.RC200.getCode());
        res.setMessage(ReturnCodeEnum.RC200.getMessage());
        res.setData(data);
        return res;
    }

    public static ResultModel fail(Integer code,String message){
        ResultModel res = new ResultModel();
        res.setCode(code);
        res.setMessage(message);
        res.setData(null);
        return res;
    }

    public boolean isSuccess() {
        return Objects.equals(ReturnCodeEnum.RC200.getCode(), code);
    }
    public T computeOrThrow(String message) {
        if (!isSuccess()) {
            throw new RuntimeException(message);
        }
        return data;
    }
}
