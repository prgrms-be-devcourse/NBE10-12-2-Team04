package com.triptrace.domain.trip.tripAuto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// AutoRecord API가 생성한 Post/Marker와 사용/제외된 이미지 수를 클라이언트에 알려주는 응답이다.
public record TripAutoRecordResponse(
    Long tripId,
    int generatedPostCount,
    int generatedMarkerCount,
    int usedImageCount,
    int skippedImageCount,
    List<GeneratedRecord> records
) {
    // 클러스터 하나가 Post 하나와 Marker 하나로 변환된 결과를 표현
    public record GeneratedRecord(
        Long postId,
        Long markerId,
        Long representativeImageId,
        String representativeThumbnailUrl,
        LocalDate date,
        BigDecimal centerLat,
        BigDecimal centerLng,
        List<Long> imageIds
    ) {
    }
}
