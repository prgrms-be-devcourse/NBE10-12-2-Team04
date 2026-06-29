package com.triptrace.domain.trip.trip.dto;

import java.time.LocalDateTime;

public record TripCreateRequest(
    String title,
    String country,
    String city,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean visibility
) {
}
