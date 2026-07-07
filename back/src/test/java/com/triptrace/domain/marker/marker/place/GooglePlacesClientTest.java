package com.triptrace.domain.marker.marker.place;

import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesClientTest {

    @Test
    @DisplayName("Google Places Nearby Search 응답을 장소 후보 목록으로 변환한다")
    void findNearbyPlacesReturnsPlaceCandidates() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("https://places.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GooglePlacesClient client = new GooglePlacesClient(
            "test-api-key",
            restClientBuilder.build()
        );

        server.expect(requestTo(startsWith("https://places.googleapis.com/v1/places:searchNearby")))
            .andExpect(header("X-Goog-Api-Key", "test-api-key"))
            .andExpect(header(
                "X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location,places.id,places.types"
            ))
            .andRespond(withSuccess("""
                {
                  "places": [
                    {
                      "id": "place-1",
                      "types": ["restaurant", "food", "point_of_interest"],
                      "formattedAddress": "380 Biryong-ro, Hwado-eup, Namyangju-si",
                      "location": {
                        "latitude": 37.6836609,
                        "longitude": 127.3073092
                      },
                      "displayName": {
                        "text": "서울해장국",
                        "languageCode": "ko"
                      }
                    },
                    {
                      "id": "place-2",
                      "types": ["convenience_store", "store"],
                      "formattedAddress": "382 Biryong-ro, Hwado-eup, Namyangju-si",
                      "location": {
                        "latitude": 37.6838111,
                        "longitude": 127.3073395
                      },
                      "displayName": {
                        "text": "gs편의점",
                        "languageCode": "ko"
                      }
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<PlaceCandidateResponse> candidates = client.findNearbyPlaces(
            new BigDecimal("37.683784021254"),
            new BigDecimal("127.307395646823")
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates.getFirst().placeId()).isEqualTo("place-1");
        assertThat(candidates.getFirst().name()).isEqualTo("서울해장국");
        assertThat(candidates.getFirst().latitude()).isEqualByComparingTo("37.6836609");
        assertThat(candidates.getFirst().types()).contains("restaurant", "food");
        server.verify();
    }

    @Test
    @DisplayName("Google Places Text Search 응답을 장소 후보 목록으로 변환한다")
    void searchPlacesReturnsPlaceCandidates() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("https://places.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GooglePlacesClient client = new GooglePlacesClient(
            "test-api-key",
            restClientBuilder.build()
        );

        server.expect(requestTo(startsWith("https://places.googleapis.com/v1/places:searchText")))
            .andExpect(header("X-Goog-Api-Key", "test-api-key"))
            .andExpect(header(
                "X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location,places.id,places.types"
            ))
            .andRespond(withSuccess("""
                {
                  "places": [
                    {
                      "id": "place-3",
                      "types": ["tourist_attraction", "point_of_interest"],
                      "formattedAddress": "부산광역시 남구 문현동",
                      "location": {
                        "latitude": 35.1481,
                        "longitude": 129.0647
                      },
                      "displayName": {
                        "text": "문현동",
                        "languageCode": "ko"
                      }
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        List<PlaceCandidateResponse> candidates = client.searchPlaces("문현동");

        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().placeId()).isEqualTo("place-3");
        assertThat(candidates.getFirst().name()).isEqualTo("문현동");
        assertThat(candidates.getFirst().address()).isEqualTo("부산광역시 남구 문현동");
        assertThat(candidates.getFirst().longitude()).isEqualByComparingTo("129.0647");
        server.verify();
    }
}
