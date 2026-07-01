package com.triptrace.domain.marker.marker.dto;

import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarkerResponse(
    Long id,
    Long postId,
    BigDecimal centerLat,
    BigDecimal centerLng,
    String placeName,
    LocalDateTime visitedAt,
    MarkerSource source,
    Long representativeImageId,
    String representativeThumbnailUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public MarkerResponse(Marker marker) {
        // 대표 이미지가 없는 수동 마커는 이미지 관련 응답값을 null로 내려줌.
        this(
            marker.getId(),
            marker.getPost().getId(),
            marker.getCenterLat(),
            marker.getCenterLng(),
            marker.getPlaceName(),
            marker.getVisitedAt(),
            marker.getSource(),
            marker.getRepresentativeImage() == null ? null : marker.getRepresentativeImage().getId(),
            marker.getRepresentativeImage() == null ? null : marker.getRepresentativeImage().getThumbnailUrl(),
            marker.getCreatedAt(),
            marker.getUpdatedAt()
        );
    }
}
