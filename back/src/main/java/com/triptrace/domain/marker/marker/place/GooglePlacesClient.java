package com.triptrace.domain.marker.marker.place;

import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GooglePlacesClient {

    private static final int MAX_RESULT_COUNT = 5;
    private static final double SEARCH_RADIUS_METERS = 100.0;
    private static final String FIELD_MASK = String.join(",",
        "places.displayName",
        "places.formattedAddress",
        "places.location",
        "places.id",
        "places.types"
    );

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public GooglePlacesClient(
        @Value("${custom.google.maps.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
            .baseUrl("https://places.googleapis.com")
            .build();
        this.apiKey = apiKey;
    }

    GooglePlacesClient(String apiKey, RestClient restClient) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public List<PlaceCandidateResponse> findNearbyPlaces(BigDecimal latitude, BigDecimal longitude) {
        if (!StringUtils.hasText(apiKey) || latitude == null || longitude == null) {
            return List.of();
        }

        try {
            JsonNode response = restClient.post()
                .uri("/v1/places:searchNearby")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(createRequestBody(latitude, longitude))
                .retrieve()
                .body(JsonNode.class);

            return extractPlaceCandidates(response);
        } catch (RestClientException e) {
            // 후보 조회 실패는 마커 수정 자체를 막을 정도의 핵심 실패가 아니므로 빈 목록으로 응답한다.
            return List.of();
        }
    }

    private Map<String, Object> createRequestBody(BigDecimal latitude, BigDecimal longitude) {
        return Map.of(
            "maxResultCount", MAX_RESULT_COUNT,
            "locationRestriction", Map.of(
                "circle", Map.of(
                    "center", Map.of(
                        "latitude", latitude,
                        "longitude", longitude
                    ),
                    "radius", SEARCH_RADIUS_METERS
                )
            )
        );
    }

    private List<PlaceCandidateResponse> extractPlaceCandidates(JsonNode response) {
        JsonNode places = response == null ? null : response.path("places");
        if (places == null || !places.isArray()) {
            return List.of();
        }

        List<PlaceCandidateResponse> candidates = new ArrayList<>();

        for (JsonNode place : places) {
            String name = place.path("displayName").path("text").asText(null);
            if (!StringUtils.hasText(name)) {
                continue;
            }

            candidates.add(new PlaceCandidateResponse(
                place.path("id").asText(null),
                name,
                place.path("formattedAddress").asText(null),
                toBigDecimal(place.path("location").path("latitude")),
                toBigDecimal(place.path("location").path("longitude")),
                extractTypes(place.path("types"))
            ));
        }

        return candidates;
    }

    private List<String> extractTypes(JsonNode types) {
        if (!types.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode type : types) {
            String value = type.asText(null);
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }

        return result;
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        String value = node.asText(null);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return new BigDecimal(value);
    }
}
