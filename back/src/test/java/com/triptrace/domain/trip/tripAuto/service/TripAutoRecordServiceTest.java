package com.triptrace.domain.trip.tripAuto.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.marker.marker.geocoding.ReverseGeocodingResult;
import com.triptrace.domain.trip.trip.entity.Trip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripAutoRecordServiceTest {

    @Test
    @DisplayName("클러스터 첫 사진과 2시간을 초과해 차이나는 이미지는 새 클러스터로 분리한다")
    void clusterImagesByClusterStartTime() {
        TripAutoRecordService tripAutoRecordService = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );
        Image first = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 14, 0));
        Image second = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 15, 50));
        Image third = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 17, 40));

        List<List<Image>> clusters = ReflectionTestUtils.invokeMethod(
            tripAutoRecordService,
            "clusterImages",
            List.of(first, second, third)
        );

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0)).containsExactly(first, second);
        assertThat(clusters.get(1)).containsExactly(third);
    }

    @Test
    @DisplayName("자동 생성 후 Trip 국가/도시는 첫 마커 기준, 기간은 첫 사진과 마지막 사진 기준으로 보정한다")
    void applyTripAutoRecordDefaults() {
        TripAutoRecordService tripAutoRecordService = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );
        Trip trip = new Trip(
            null,
            "부산 여행",
            "기존 국가",
            "기존 도시",
            LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 1, 2, 0, 0),
            true
        );
        Image first = imageCapturedAt(LocalDateTime.of(2026, 6, 27, 19, 37, 55));
        Image last = imageCapturedAt(LocalDateTime.of(2026, 6, 30, 10, 15));

        ReflectionTestUtils.invokeMethod(
            tripAutoRecordService,
            "applyTripAutoRecordDefaults",
            trip,
            List.of(first, last),
            new ReverseGeocodingResult("대한민국", "부산광역시", "부산광역시 남구 문현동")
        );

        assertThat(trip.getCountry()).isEqualTo("대한민국");
        assertThat(trip.getCity()).isEqualTo("부산광역시");
        assertThat(trip.getStartDate()).isEqualTo(LocalDateTime.of(2026, 6, 27, 19, 37, 55));
        assertThat(trip.getEndDate()).isEqualTo(LocalDateTime.of(2026, 6, 30, 10, 15));
    }

    @Test
    @DisplayName("자동 생성 마커 중심 좌표는 소수점 7자리까지 버림 처리한다")
    void truncateCoordinateToSevenDecimalPlaces() {
        TripAutoRecordService tripAutoRecordService = new TripAutoRecordService(
            null,
            null,
            null,
            null,
            null
        );

        BigDecimal coordinate = ReflectionTestUtils.invokeMethod(
            tripAutoRecordService,
            "truncateCoordinate",
            new BigDecimal("37.123456789")
        );

        assertThat(coordinate).isEqualByComparingTo(new BigDecimal("37.1234567"));
    }

    private Image imageCapturedAt(LocalDateTime capturedAt) {
        Image image = new Image(
            null,
            null,
            null,
            "https://example.com/image.jpg",
            "https://example.com/image-thumbnail.jpg",
            1000L,
            "image/jpeg",
            UploadStatus.STORED
        );
        ReflectionTestUtils.setField(image, "capturedAt", capturedAt);

        return image;
    }
}
