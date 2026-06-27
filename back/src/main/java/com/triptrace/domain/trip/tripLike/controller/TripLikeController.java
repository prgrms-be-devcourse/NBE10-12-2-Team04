package com.triptrace.domain.trip.tripLike.controller;

import com.triptrace.domain.trip.tripLike.dto.TripLikeResponse;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<TripLikeResponse> isLiked(
        @RequestParam Long memberId,
        @PathVariable Long tripId
    ) {
        List<TripLike> tripLikeList = tripLikeService.findAll();

        return tripLikeList
            .stream()
            .map(TripLikeResponse::new)
            .toList();
    }
}
