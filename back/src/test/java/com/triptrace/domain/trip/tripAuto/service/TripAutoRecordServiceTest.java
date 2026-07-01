package com.triptrace.domain.trip.tripAuto.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
