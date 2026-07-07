package com.triptrace.domain.marker.marker.geocoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleReverseGeocodingClientTest {

    @Test
    @DisplayName("Google 역지오코딩 응답에서 도로명 주소 대신 행정구역명을 추출한다")
    void findPlaceNameReturnsRegionName() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("https://maps.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GoogleReverseGeocodingClient client = new GoogleReverseGeocodingClient(
            "test-api-key",
            restClientBuilder.build()
        );

        server.expect(requestTo(startsWith("https://maps.googleapis.com/maps/api/geocode/json")))
            .andExpect(queryParam("latlng", "37.683784021254,127.307395646823"))
            .andExpect(queryParam("language", "ko"))
            .andExpect(queryParam("key", "test-api-key"))
            .andRespond(withSuccess("""
                {
                  "results": [
                    {
                      "address_components": [
                        {
                          "long_name": "382",
                          "types": ["premise"]
                        },
                        {
                          "long_name": "비룡로",
                          "types": ["political", "sublocality", "sublocality_level_4"]
                        },
                        {
                          "long_name": "화도읍",
                          "types": ["political", "sublocality", "sublocality_level_2"]
                        },
                        {
                          "long_name": "남양주시",
                          "types": ["locality", "political"]
                        },
                        {
                          "long_name": "경기도",
                          "types": ["administrative_area_level_1", "political"]
                        },
                        {
                          "long_name": "대한민국",
                          "types": ["country", "political"]
                        }
                      ],
                      "formatted_address": "대한민국 경기도 남양주시 화도읍 비룡로 382"
                    }
                  ],
                  "status": "OK"
                }
                """, MediaType.APPLICATION_JSON));

        String placeName = client.findPlaceName(
            new BigDecimal("37.683784021254"),
            new BigDecimal("127.307395646823")
        );

        assertThat(placeName).isEqualTo("경기도 남양주시 화도읍");
        server.verify();
    }

    @Test
    @DisplayName("Google 역지오코딩 응답에서 국가, 도시, 장소명을 분리한다")
    void findLocationReturnsCountryCityAndPlaceName() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("https://maps.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GoogleReverseGeocodingClient client = new GoogleReverseGeocodingClient(
            "test-api-key",
            restClientBuilder.build()
        );

        server.expect(requestTo(startsWith("https://maps.googleapis.com/maps/api/geocode/json")))
            .andExpect(queryParam("latlng", "35.1481,129.0647"))
            .andExpect(queryParam("language", "ko"))
            .andExpect(queryParam("key", "test-api-key"))
            .andRespond(withSuccess("""
                {
                  "results": [
                    {
                      "address_components": [
                        {
                          "long_name": "문현동",
                          "types": ["political", "sublocality", "sublocality_level_2"]
                        },
                        {
                          "long_name": "남구",
                          "types": ["political", "sublocality", "sublocality_level_1"]
                        },
                        {
                          "long_name": "부산광역시",
                          "types": ["locality", "political"]
                        },
                        {
                          "long_name": "대한민국",
                          "types": ["country", "political"]
                        }
                      ],
                      "formatted_address": "대한민국 부산광역시 남구 문현동"
                    }
                  ],
                  "status": "OK"
                }
                """, MediaType.APPLICATION_JSON));

        ReverseGeocodingResult location = client.findLocation(
            new BigDecimal("35.1481"),
            new BigDecimal("129.0647")
        );

        assertThat(location.country()).isEqualTo("한국");
        assertThat(location.city()).isEqualTo("부산광역시");
        assertThat(location.placeName()).isEqualTo("부산광역시 남구 문현동");
        server.verify();
    }
}
