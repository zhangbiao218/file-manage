package com.tiansuo.file.manage.exception;


import com.tiansuo.file.manage.enums.MinioPlusErrorCode;

/**
 * MinioPlus专用异常定义
 * @author zhangb
 */
public class MinioPlusException extends RuntimeException {

    private static final long serialVersionUID = 772046747932011086L;

    private int errorCode;

    private String errorMessage;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public MinioPlusException() {
        super();
    }

    public MinioPlusException(String message) {
        super(message);
    }

    public MinioPlusException(MinioPlusErrorCode minioPlusErrorCode){
        this.errorCode = minioPlusErrorCode.getCode();
        this.errorMessage = minioPlusErrorCode.getMessage();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public MinioPlusException(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
