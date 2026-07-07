package com.triptrace.domain.marker.marker.dto;

import com.triptrace.domain.marker.marker.entity.MarkerSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkerCreateRequest(

    BigDecimal centerLat,

    BigDecimal centerLng,

    @Size(max = 100)
    String placeName,

    LocalDateTime visitedAt,

    @NotNull
    MarkerSource source
) {
}
