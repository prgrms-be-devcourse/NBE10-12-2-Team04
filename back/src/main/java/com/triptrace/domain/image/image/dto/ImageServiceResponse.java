package com.triptrace.domain.image.image.dto;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ImageServiceResponse(
    Long ownerId,
    Long tripId,
    Long postId,
    Long markerId,
    String originalFileUrl,
    String thumbnailUrl,
    String mimeType,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDateTime capturedAt,
    String deviceInfo,
    UploadStatus uploadStatus
) {
    public ImageServiceResponse(Image image){
        //gps값 정확도 희석
        BigDecimal latitude = image.getGpsLat().setScale(4, RoundingMode.FLOOR);
        BigDecimal longitude = image.getGpsLat().setScale(4, RoundingMode.FLOOR);
        this(
            image.getOwner().getId(),
            image.getTrip().getId(),
            image.getPost().getId(),
            image.getMarker().getId(),
            image.getOriginalFileUrl(),
            image.getThumbnailUrl(),
            image.getMimeType(),
            image.getGpsLat(),
            image.getGpsLng(),
            image.getCapturedAt(),
            image.getDeviceInfo(),
            image.getUploadStatus()
        );
    }
}
