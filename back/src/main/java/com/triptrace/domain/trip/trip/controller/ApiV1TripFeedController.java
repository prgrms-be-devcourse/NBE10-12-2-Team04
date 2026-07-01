package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feed/trips")
@RequiredArgsConstructor
public class ApiV1TripFeedController {
    private final TripService tripService;

    @GetMapping("/top-liked")
    public RsData<List<TripResponse>> getLikedTop10(
        @RequestParam(value = "limit", defaultValue = "10") Long likeCount
    ) {
        return new RsData<> (
            "200-1",
            "좋아요 상위 10개 여행기 조회에 성공했습니다.",
            tripService.findPublicTrips()
        );
    }
}
