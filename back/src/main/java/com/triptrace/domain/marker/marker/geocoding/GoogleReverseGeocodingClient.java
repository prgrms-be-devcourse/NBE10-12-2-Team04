package com.triptrace.domain.marker.marker.geocoding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleReverseGeocodingClient implements ReverseGeocodingClient {

    private static final int MAX_PLACE_NAME_LENGTH = 100;
    private static final List<String> REGION_TYPE_PRIORITY = List.of(
        "administrative_area_level_1",
        "administrative_area_level_2",
        "locality",
        "sublocality_level_1",
        "sublocality_level_2",
        "sublocality_level_3"
    );

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public GoogleReverseGeocodingClient(
        @Value("${custom.google.maps.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
            .baseUrl("https://maps.googleapis.com")
            .build();
        this.apiKey = apiKey;
    }

    GoogleReverseGeocodingClient(String apiKey, RestClient restClient) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public String findPlaceName(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) {
            return null;
        }

        try {
            JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/maps/api/geocode/json")
                    .queryParam("latlng", "%s,%s".formatted(latitude, longitude))
                    .queryParam("language", "ko")
                    .queryParam("key", apiKey)
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
        if (response == null || !"OK".equals(response.path("status").asText())) {
            return null;
        }

        JsonNode firstResult = response.path("results").path(0);
        if (firstResult.isMissingNode()) {
            return null;
        }

        String regionName = extractRegionName(firstResult.path("address_components"));
        if (StringUtils.hasText(regionName)) {
            return trimPlaceName(regionName);
        }

        return trimPlaceName(firstResult.path("formatted_address").asText(null));
    }

    private String extractRegionName(JsonNode addressComponents) {
        if (!addressComponents.isArray()) {
            return null;
        }

        // 자동 생성 단계에서는 정확하지 않을 수 있는 상호명/도로명 대신
        // 사용자가 나중에 장소명을 선택하기 전까지 보여줄 행정구역명만 사용한다.
        List<String> regionParts = new ArrayList<>();

        for (String regionType : REGION_TYPE_PRIORITY) {
            String regionPart = findLongNameByType(addressComponents, regionType);
            if (StringUtils.hasText(regionPart) && !regionParts.contains(regionPart)) {
                regionParts.add(regionPart);
            }
        }

        if (regionParts.isEmpty()) {
            return null;
        }

        return String.join(" ", regionParts);
    }

    private String findLongNameByType(JsonNode addressComponents, String targetType) {
        for (JsonNode component : addressComponents) {
            JsonNode types = component.path("types");
            if (hasType(types, targetType)) {
                return component.path("long_name").asText(null);
            }
        }

        return null;
    }

    private boolean hasType(JsonNode types, String targetType) {
        if (!types.isArray()) {
            return false;
        }

        for (JsonNode type : types) {
            if (targetType.equals(type.asText())) {
                return true;
            }
        }

        return false;
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
