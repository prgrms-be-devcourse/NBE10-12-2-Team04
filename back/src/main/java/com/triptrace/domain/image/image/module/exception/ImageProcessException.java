package com.triptrace.domain.image.image.module.exception;

public class ImageProcessException extends RuntimeException {
    //TODO: GLOBAL EXCEPTION 에 등록해야함
    private final String resultCode;
    private final String msg;
    public ImageProcessException(String resultCode, String message) {
        super(message);
        this.msg = message;
        this.resultCode = resultCode;
    }
    public ImageProcessException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
        this.msg = message;
    }
}
