package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.trip.trip.dto.TripCreateReqBody;
import com.triptrace.domain.trip.trip.dto.TripModifyReqBody;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1TripController {
    private final TripService tripService;

    @PostMapping("/trips")
    public RsData<TripResponse> create(
        @RequestBody TripCreateReqBody reqBody
    ) {
        TripResponse response = tripService.create(reqBody);

        return new RsData<>(
            "201-1",
            "%d번 여행기가 생성되었습니다.".formatted(response.id()),
            response
        );
    }

    @GetMapping("/users/me/trips")
    public RsData<List<TripResponse>> mine(
        @RequestParam Long memberId
    ) {
        return new RsData<>(
            "200-1",
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findMine(memberId)
        );
    }

    @GetMapping("/trips/{tripId}")
    public RsData<TripResponse> detail(
        @PathVariable Long tripId
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기 조회에 성공했습니다.".formatted(tripId),
            tripService.findById(tripId)
        );
    }

    @PatchMapping("/trips/{tripId}")
    public RsData<TripResponse> modify(
        @PathVariable Long tripId,
        @RequestBody TripModifyReqBody reqBody
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기가 수정되었습니다.".formatted(tripId),
            tripService.modify(tripId, reqBody)
        );
    }

    @DeleteMapping("/trips/{tripId}")
    public RsData<Void> delete(
        @PathVariable Long tripId,
        @RequestParam Long memberId
    ) {
        tripService.delete(tripId, memberId);

        return new RsData<>(
            "200-1",
            "%d번 여행기가 삭제되었습니다.".formatted(tripId)
        );
    }
}
