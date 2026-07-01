package com.triptrace.domain.marker.marker.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceCandidateResponse(
    String placeId,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    List<String> types
) {
}
