package com.triptrace.domain.marker.marker.geocoding;

import java.math.BigDecimal;

public interface ReverseGeocodingClient {

    String findPlaceName(BigDecimal latitude, BigDecimal longitude);
}
