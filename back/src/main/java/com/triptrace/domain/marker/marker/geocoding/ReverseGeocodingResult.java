package com.triptrace.domain.marker.marker.geocoding;

public record ReverseGeocodingResult(
    String country,
    String city,
    String placeName
) {
}
