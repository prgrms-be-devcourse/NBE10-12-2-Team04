package com.triptrace.domain.image.image.storage;


public record StoredImageFile(
    String imageFileUrl,
    String thumbnailImageFileUrl,
    Long fileSize,
    String mimeType
) {
}
