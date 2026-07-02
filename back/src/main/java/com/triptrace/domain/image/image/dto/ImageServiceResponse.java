package com.triptrace.domain.image.image.dto;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ImageServiceResponse(
    Long id,
    Member owner,
    Trip trip,
    Post post,
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
