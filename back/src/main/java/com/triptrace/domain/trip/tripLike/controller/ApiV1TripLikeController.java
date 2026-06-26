package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class ApiV1TripLikeController {
    private final TripLikeService tripLikeService;


    @PostMapping("/{tripId}/likes")
    @Transactional
    public RsData<Void> createLike(Long memberId, Long tripId) {
        tripLikeService.createLike(memberId, tripId);

        return new RsData<>(
            "201-1",
            "%d번 여행기에 좋아요가 등록되었습니다.".formatted(tripId)
        );
    }
}
