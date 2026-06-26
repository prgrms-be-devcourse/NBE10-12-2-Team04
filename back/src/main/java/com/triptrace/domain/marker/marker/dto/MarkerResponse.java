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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public MarkerResponse(Marker marker) {
        this(
            marker.getId(),
            marker.getPost().getId(),
            marker.getCenterLat(),
            marker.getCenterLng(),
            marker.getPlaceName(),
            marker.getVisitedAt(),
            marker.getSource(),
            marker.getCreatedAt(),
            marker.getUpdatedAt()
        );
    }
}
