package com.triptrace.domain.marker.marker.controller;

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest;
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest;
import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import com.triptrace.domain.marker.marker.service.MarkerService;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MarkerController {

    private final MarkerService markerService;

    // 마커 생성
    @PostMapping("/posts/{postId}/markers")
    public RsData<MarkerResponse> createMarker(
        @PathVariable Long postId,
        @AuthenticationPrincipal Long memberId,
        @Valid @RequestBody MarkerCreateRequest request
    ) {

        MarkerResponse response = markerService.createMarker(postId, memberId, request);

        return new RsData<>(
            "201-1",
            "마커가 생성되었습니다.",
            response
        );
    }

    // 마커 목록 조회
    @GetMapping("/posts/{postId}/markers")
    public RsData<List<MarkerResponse>> getMarkers (
        @PathVariable Long postId
    ) {

        return new RsData<>(
            "200-1",
            "마커 목록 조회에 성공했습니다.",
            markerService.getMarkers(postId)
        );
    }

    // 마커 상세 조회
    @GetMapping("/posts/markers/{markerId}")
    public RsData<MarkerResponse> getMarker (
        @PathVariable Long markerId
    ) {

        return new RsData<>(
            "200-1",
            "마커 조회에 성공했습니다.",
            markerService.getMarker(markerId)
        );
    }

    // 마커 장소명 후보 조회
    @GetMapping("/posts/markers/{markerId}/place-candidates")
    public RsData<List<PlaceCandidateResponse>> getPlaceCandidates(
        @PathVariable Long markerId,
        @AuthenticationPrincipal Long memberId
    ) {

        return new RsData<>(
            "200-1",
            "마커 장소명 후보 조회에 성공했습니다.",
            markerService.getPlaceCandidates(markerId, memberId)
        );
    }

    // 장소 검색
    @GetMapping("/places/search")
    public RsData<List<PlaceCandidateResponse>> searchPlaces(
        @RequestParam String keyword
    ) {

        return new RsData<>(
            "200-1",
            "장소 검색에 성공했습니다.",
            markerService.searchPlaces(keyword)
        );
    }

    // 좌표 기준 주변 장소 조회
    @GetMapping("/places/nearby")
    public RsData<List<PlaceCandidateResponse>> findNearbyPlaces(
        @RequestParam BigDecimal latitude,
        @RequestParam BigDecimal longitude
    ) {

        return new RsData<>(
            "200-1",
            "주변 장소 조회에 성공했습니다.",
            markerService.findNearbyPlaces(latitude, longitude)
        );
    }

    // 마커 수정
    @PatchMapping("/posts/markers/{markerId}")
    public RsData<MarkerResponse> modifyMarker (
        @PathVariable Long markerId,
        @AuthenticationPrincipal Long memberId,
        @Valid @RequestBody MarkerModifyRequest request
    ) {

        return new RsData<>(
            "200-1",
            "마커가 수정되었습니다.",
            markerService.modifyMarker(markerId, memberId, request)
        );
    }

    @DeleteMapping("/posts/markers/{markerId}")
    public RsData<Void> deleteMarker (
        @PathVariable Long markerId,
        @AuthenticationPrincipal Long memberId
    ) {
        markerService.deleteMarker(markerId, memberId);
        return new RsData<>("400-1", "마커는 삭제할 수 없습니다.", null);
    }
}
