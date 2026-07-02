package com.triptrace.global.exception;

import com.triptrace.domain.image.image.catalog.ErrorCode;
import com.triptrace.global.rsData.RsData;

public class ServiceException extends RuntimeException {
    private final String resultCode;
    private final String msg;

    public ServiceException(String resultCode, String msg) {
        super(resultCode + " : " + msg);
        this.resultCode = resultCode;
        this.msg = msg;
    }
    public ServiceException(ErrorCode errorCode) {
        this(errorCode.getResultCode(), errorCode.getMessage());
    }

    public RsData<Void> getRsData() {
        return new RsData<>(resultCode, msg, null);
    }
}
