package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripLikeController {
    private final TripLikeService tripLikeService;


    @PostMapping("/{tripId}/likes")
    @Transactional
    public RsData<Void> createLike(
        @RequestParam Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.createLike(memberId, tripId);

        return new RsData<>(
            "201-1",
            "좋아요가 등록되었습니다."
        );
    }

    @DeleteMapping("/{tripId}/likes")
    @Transactional
    public RsData<Void> deleteLike(
        @RequestParam Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.deleteLike(memberId, tripId);

        return new RsData<>(
            "200-1",
            "좋아요가 취소되었습니다."
        );
    }

}
