package com.triptrace.domain.image.image.dto.response;

import com.triptrace.domain.image.image.entity.UploadStatus;


public record ImageUploadResponse(
    String fileName,
    Long id,
    String originalFileUrl,
    String thumbnailUrl,
    String mimeType,
    UploadStatus uploadStatus,
    String message
) {
}
