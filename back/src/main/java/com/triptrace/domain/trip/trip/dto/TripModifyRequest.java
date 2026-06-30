package com.triptrace.domain.trip.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TripModifyRequest(
    @NotBlank
    @Size(max = 100)
    String title,
    @Size(max = 100)
    String country,
    @Size(max = 100)
    String city,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean visibility
) {
}
