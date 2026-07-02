package com.triptrace.domain.image.image.catalog;

public enum ImageErrorCode implements ErrorCode {
    INVALID_VALUE("400-1","요청값이 올바르지 않습니다."),
    UNAUTHORIZED("401-1","인증에 실패했습니다."),
    FORBIDDEN("403-1", "권한이 없습니다."),
    NOT_FOUND("404-1","이미지를 찾을 수 없습니다."),
    DUPLICATE("409-1","중복된 이미지입니다.");

    public final String resultCode;
    public final String message;
    ImageErrorCode(String code, String message){
        this.resultCode = code;
        this.message = message;
    }
    public String getResultCode() {
        return resultCode;
    }
    public String getMessage() {
        return message;
    }
}
