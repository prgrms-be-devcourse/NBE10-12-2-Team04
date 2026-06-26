package com.triptrace.domain.trip.tripLike.dto;

import java.time.LocalDateTime;

public record TripLikeReqBody(
    Long id,
    Long memberId,
    Long tripId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
    ) {
}
