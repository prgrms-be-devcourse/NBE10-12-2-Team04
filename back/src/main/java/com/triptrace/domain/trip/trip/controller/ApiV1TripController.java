package com.triptrace.domain.trip.trip.controller;

import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
import com.triptrace.domain.trip.trip.dto.TripRepresentativeImageRequest;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1TripController {
    private final TripService tripService;

    @PostMapping("/trips")
    public RsData<TripResponse> createTrip(
        @AuthenticationPrincipal Long memberId,
        @RequestBody @Valid TripCreateRequest request
    ) {
        TripResponse response = tripService.create(memberId, request);

        return new RsData<>(
            "201-1",
            "%d번 여행기가 생성되었습니다.".formatted(response.id()),
            response
        );
    }

    @GetMapping("/users/me/trips")
    public RsData<List<TripResponse>> getMyTrips(
        @AuthenticationPrincipal Long memberId
    ) {
        return new RsData<>(
            "200-1",
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findTripsByOwnerId(memberId)
        );
    }

    @GetMapping(value = "/users/me/trips", params = {"page", "size"})
    public RsData<Page<TripResponse>> getMyTrips(
        @AuthenticationPrincipal Long memberId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return new RsData<>(
            "200-1",
            "내 여행기 목록 조회에 성공했습니다.",
            tripService.findTripsByOwnerId(memberId, pageable)
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
        @AuthenticationPrincipal Long memberId
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기 조회에 성공했습니다.".formatted(tripId),
            tripService.findAccessibleTrip(tripId, memberId)
        );
    }

    @PatchMapping("/trips/{tripId}")
    public RsData<TripResponse> modifyTrip(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Long memberId,
        @RequestBody @Valid TripModifyRequest request
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기가 수정되었습니다.".formatted(tripId),
            tripService.modifyTrip(tripId, memberId, request)
        );
    }

    @DeleteMapping("/trips/{tripId}")
    public RsData<Void> deleteTrip(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Long memberId
    ) {
        tripService.deleteTrip(tripId, memberId);

        return new RsData<>(
            "200-1",
            "%d번 여행기가 삭제되었습니다.".formatted(tripId)
        );
    }

    @PatchMapping("/trips/{tripId}/representative-image")
    public RsData<TripResponse> changeRepresentativeImage(
        @PathVariable Long tripId,
        @AuthenticationPrincipal Long memberId,
        @RequestBody @Valid TripRepresentativeImageRequest request
    ) {
        return new RsData<>(
            "200-1",
            "%d번 여행기 대표이미지가 수정되었습니다.".formatted(tripId),
            tripService.changeRepresentativeImage(tripId, memberId, request.imageId())
        );
    }
}
