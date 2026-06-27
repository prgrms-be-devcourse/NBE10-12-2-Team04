package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.trip.tripLike.dto.TripLikeStatusResponse;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripLikeController {
    private final TripLikeService tripLikeService;

    // 좋아요 추가
    @PostMapping("/{tripId}/likes")
    public RsData<Void> createLike(
        @RequestParam Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.createLike(memberId, tripId);

        return new RsData<>(
            "201-1",
            "좋아요가 등록되었습니다."
        );
    }

    // 좋아요 취소
    @DeleteMapping("/{tripId}/likes")
    public RsData<Void> deleteLike(
        @RequestParam Long memberId,
        @PathVariable Long tripId) {

        tripLikeService.deleteLike(memberId, tripId);

        return new RsData<>(
            "200-1",
            "좋아요가 취소되었습니다."
        );
    }

    // 좋아요 여부 조회
    @GetMapping("{tripId}/likes/me")
    public RsData<TripLikeStatusResponse> isLiked(
        @RequestParam Long memberId,
        @PathVariable Long tripId
    ) {
        boolean liked = tripLikeService.isLiked(memberId, tripId);

        return new RsData<>(
            "200-1",
            "좋아요 여부 조회 성공했습니다.",
            new TripLikeStatusResponse(liked)
        );
    }
}
