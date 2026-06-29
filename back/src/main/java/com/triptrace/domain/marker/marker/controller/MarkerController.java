package com.triptrace.domain.marker.marker.controller;

import com.triptrace.domain.marker.marker.dto.MarkerCreateReqBody;
import com.triptrace.domain.marker.marker.dto.MarkerModifyReqBody;
import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.service.MarkerService;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MarkerController {

    private final MarkerService markerService;

    // 마커 생성
    @PostMapping("/posts/{postId}/markers")
    public RsData<MarkerResponse> create(
        @PathVariable Long postId,
        @RequestBody MarkerCreateReqBody reqBody
    ) {
        MarkerResponse response =
            markerService.createMarker(postId, reqBody);

        return new RsData<>(
            "201-1",
            "마커가 생성되었습니다.",
            response
        );
    }

    // 마커 목록 조회
    @GetMapping("/posts/{postId}/markers")
    public RsData<List<MarkerResponse>> list(
        @PathVariable Long postId
    ) {
        return  new RsData<>(
            "200-1",
            "마커 목록 조회 성공",
            markerService.getMarkers(postId)
        );
    }

    // 마커 상세 조회
    @GetMapping("/posts/markers/{markerId}")
    public RsData<MarkerResponse> detail(
        @PathVariable Long markerId
    ) {
        return  new RsData<>(
            "200-1",
            "마커 조회 성공",
            markerService.getMarker(markerId)
        );
    }

    // 마커 수정
    @PatchMapping("/posts/markers/{markerId}")
    public RsData<MarkerResponse> modify(
        @PathVariable Long markerId,
        @RequestBody MarkerModifyReqBody reqBody
    ) {
        return new RsData<>(
            "200-1",
            "마커가 수정되었습니다.",
            markerService.modifyMarker(markerId, reqBody)
        );
    }

    // 마커 삭제
    @DeleteMapping("/posts/markers/{markerId}")
    public RsData<Void> delete(
        @PathVariable Long markerId,
        @RequestBody Long memberId
    ) {
        markerService.deleteMarker(markerId, memberId);

        return new RsData<>(
            "200-1",
            "마커가 삭제되었습니다.",
            null
        );
    }
}
