package com.triptrace.domain.image.image.dto;


public record ImageFileRequest (
    String imageFileUrl,
    String thumbnailImageFileUrl,
    Long fileSize,
    String mimeType
    ){
}
