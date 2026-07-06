package com.triptrace.domain.trip.trip.dto;

import com.triptrace.domain.trip.trip.entity.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponse(
    Long id,
    Long ownerId,
    AuthorResponse author,
    String thumbnailUrl,
    BigDecimal representativeLat,
    BigDecimal representativeLng,
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
            new AuthorResponse(
                trip.getOwner().getId(),
                trip.getOwner().getUsername(),
                trip.getOwner().getProfileImageUrl()
            ),
            trip.getRepresentativeImage() == null
                ? null : trip.getRepresentativeImage().getThumbnailUrl(),
            trip.getRepresentativeImage() == null ? null : trip.getRepresentativeImage().getGpsLat(),
            trip.getRepresentativeImage() == null ? null : trip.getRepresentativeImage().getGpsLng(),
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

    public record AuthorResponse(
        Long id,
        String nickname,
        String profileImageUrl
    ) {
    }
}
