package com.triptrace.domain.trip.trip.dto;

import jakarta.validation.constraints.NotNull;

public record TripRepresentativeImageRequest(
    @NotNull
    Long imageId
) {
}
