package com.triptrace.domain.image.image.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.triptrace.domain.image.image.entity.UploadStatus;

public record ImageServiceResponse(
    Long id,
    Long ownerId,
    Long tripId,
    Long postId,
    String originalFileUrl,
    String thumbnailUrl,
    String mimeType,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDateTime capturedAt,
    String deviceInfo,
    UploadStatus uploadStatus
) {
}
