package com.triptrace.domain.image.image.processing.exception;

public class ImageProcessException extends RuntimeException {
    private final String resultCode;
    private final String msg;
    public ImageProcessException(String resultCode, String message) {
        super(resultCode + " : " + message);
        this.msg = message;
        this.resultCode = resultCode;
    }
    public ImageProcessException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
        this.msg = message;
    }
    public String getResultCode() {
        return resultCode;
    }

    public String getMsg() {
        return msg;
    }
}
