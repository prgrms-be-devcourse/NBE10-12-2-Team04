package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
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
        @RequestParam Long ownerId,
        @RequestBody TripCreateRequest request
    ) {
        TripResponse response = tripService.create(ownerId, request);

        return new RsData<>(
            "201-1",
            "%d번 여행기가 생성되었습니다.".formatted(response.id()),
            response
        );
    }

    @GetMapping("/users/me/trips")
    public RsData<List<TripResponse>> getMyTrips(
        @RequestParam Long ownerId
    ) {
        return new RsData<>(
            "200-1",
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findTripsByOwnerId(ownerId)
        );
    }

    @GetMapping("/trips")
    public RsData<List<TripResponse>> getTrips() {
        return new RsData<>(
            "200-1",
            "공개 여행기 목록 조회에 성공했습니다.",
            tripService.findPublicTrips()
        );
    }

    @GetMapping("/trips/{tripId}")
    public RsData<TripResponse> getTrip(
        @PathVariable Long tripId,
        @RequestParam(required = false) Long ownerId
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기 조회에 성공했습니다.".formatted(tripId),
            tripService.findAccessibleTrip(tripId, ownerId)
        );
    }

    @PatchMapping("/trips/{tripId}")
    public RsData<TripResponse> modifyTrip(
        @PathVariable Long tripId,
        @RequestParam Long ownerId,
        @RequestBody TripModifyRequest request
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기가 수정되었습니다.".formatted(tripId),
            tripService.modifyTrip(tripId, ownerId, request)
        );
    }

    @DeleteMapping("/trips/{tripId}")
    public RsData<Void> deleteTrip(
        @PathVariable Long tripId,
        @RequestParam Long ownerId
    ) {
        tripService.deleteTrip(tripId, ownerId);

        return new RsData<>(
            "200-1",
            "%d번 여행기가 삭제되었습니다.".formatted(tripId)
        );
    }
}
