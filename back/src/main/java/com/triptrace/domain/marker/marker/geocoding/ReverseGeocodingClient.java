package com.triptrace.domain.marker.marker.geocoding;

import java.math.BigDecimal;

public interface ReverseGeocodingClient {

    String findPlaceName(BigDecimal latitude, BigDecimal longitude);

    default ReverseGeocodingResult findLocation(BigDecimal latitude, BigDecimal longitude) {
        return new ReverseGeocodingResult(null, null, findPlaceName(latitude, longitude));
    }
}
