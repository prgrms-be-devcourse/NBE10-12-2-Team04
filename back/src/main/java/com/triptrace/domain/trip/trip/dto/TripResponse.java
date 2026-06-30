package com.triptrace.domain.trip.trip.dto;

import com.triptrace.domain.trip.trip.entity.Trip;

import java.time.LocalDateTime;

public record TripResponse(
    Long id,
    Long ownerId,
    String title,
    String country,
    String city,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean visibility,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long likeCount
) {
    public TripResponse(Trip trip) {
        this(
            trip.getId(),
            trip.getOwner().getId(),
            trip.getTitle(),
            trip.getCountry(),
            trip.getCity(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.isVisibility(),
            trip.getCreatedAt(),
            trip.getUpdatedAt(),
            trip.getLikeCount()
        );
    }
}
