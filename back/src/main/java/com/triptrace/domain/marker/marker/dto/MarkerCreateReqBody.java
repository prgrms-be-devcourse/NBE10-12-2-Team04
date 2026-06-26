package com.triptrace.domain.marker.marker.dto;

import com.triptrace.domain.marker.marker.entity.MarkerSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarkerCreateReqBody(
    Long memberId,
    BigDecimal centerLat,
    BigDecimal centerLng,
    String placeName,
    LocalDateTime visitedAt,
    MarkerSource source
) {
}
