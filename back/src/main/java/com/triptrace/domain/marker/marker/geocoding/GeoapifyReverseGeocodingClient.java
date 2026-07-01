package com.triptrace.domain.marker.marker.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Component
public class GeoapifyReverseGeocodingClient implements ReverseGeocodingClient {

    private static final int MAX_PLACE_NAME_LENGTH = 100;

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyReverseGeocodingClient(
        @Value("${custom.geoapify.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
            .baseUrl("https://api.geoapify.com")
            .build();
        this.apiKey = apiKey;
        System.out.println("Geoapify API key exists = " + StringUtils.hasText(apiKey));
    }

    @Override
    public String findPlaceName(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) {
            return null;
        }

        try {
            JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/geocode/reverse")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("lang", "ko")
                    .queryParam("apiKey", apiKey)
                    .build()
                )
                .retrieve()
                .body(JsonNode.class);

            return extractPlaceName(response);
        } catch (RestClientException e) {
            // 장소명 조회 실패가 자동 기록 생성 전체 실패로 이어지지 않도록 비워 둔다.
            return null;
        }
    }

    private String extractPlaceName(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode properties = response.path("features").path(0).path("properties");
        if (properties.isMissingNode()) {
            return null;
        }

        String formatted = properties.path("formatted").asText(null);
        if (StringUtils.hasText(formatted)) {
            return trimPlaceName(formatted);
        }

        return trimPlaceName(buildFallbackPlaceName(properties));
    }

    private String buildFallbackPlaceName(JsonNode properties) {
        List<String> parts = List.of(
            properties.path("city").asText(""),
            properties.path("county").asText(""),
            properties.path("state").asText(""),
            properties.path("country").asText("")
        );

        return parts.stream()
            .filter(StringUtils::hasText)
            .distinct()
            .findFirst()
            .orElse(null);
    }

    private String trimPlaceName(String placeName) {
        if (!StringUtils.hasText(placeName)) {
            return null;
        }

        if (placeName.length() <= MAX_PLACE_NAME_LENGTH) {
            return placeName;
        }

        return placeName.substring(0, MAX_PLACE_NAME_LENGTH);
    }
}
