package com.triptrace.domain.image.image.catalog;

import com.triptrace.global.exception.ServiceException;

public final class ImageExceptionCatalog {
    public static ServiceException invalid(){
        return new ServiceException(ImageErrorCode.INVALID_VALUE);
    }
    public static ServiceException invalid(String message){
        return new ServiceException(ImageErrorCode.INVALID_VALUE.getResultCode(),message);
    }
    public static ServiceException forbidden(){
        return new ServiceException(ImageErrorCode.FORBIDDEN);
    }
    public static ServiceException forbidden(String message){
        return new ServiceException(ImageErrorCode.FORBIDDEN.getResultCode(),message);
    }
    public static ServiceException unauthorized(){
        return new ServiceException(ImageErrorCode.UNAUTHORIZED);
    }
    public static ServiceException unauthorized(String message){
        return new ServiceException(ImageErrorCode.UNAUTHORIZED.getResultCode(),message);
    }
    public static ServiceException notFound(){
        return new ServiceException(ImageErrorCode.NOT_FOUND);
    }
    public static ServiceException notFound(String message){
        return new ServiceException(ImageErrorCode.NOT_FOUND.getResultCode(),message);
    }
    public static ServiceException duplicate(){
        return new ServiceException(ImageErrorCode.DUPLICATE);
    }
    public static ServiceException duplicate(String message){
        return new ServiceException(ImageErrorCode.DUPLICATE.getResultCode(),message);
    }
}
