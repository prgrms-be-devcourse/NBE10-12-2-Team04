package com.triptrace.domain.trip.tripLike.dto;

import com.triptrace.domain.trip.tripLike.entity.TripLike;

import java.time.LocalDateTime;

public record TripLikeResponse(
    Long id,
    Long memberId,
    Long tripId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public TripLikeResponse(TripLike tripLike) {
        this(
            tripLike.getId(),
            tripLike.getMember().getId(),
            tripLike.getTrip().getId(),
            tripLike.getCreatedAt(),
            tripLike.getUpdatedAt()
        );
    }
}
