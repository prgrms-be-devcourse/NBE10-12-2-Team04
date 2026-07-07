package com.triptrace.domain.marker.marker.service;

import com.triptrace.domain.marker.marker.dto.MarkerCreateRequest;
import com.triptrace.domain.marker.marker.dto.MarkerModifyRequest;
import com.triptrace.domain.marker.marker.dto.MarkerResponse;
import com.triptrace.domain.marker.marker.dto.PlaceCandidateResponse;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.place.GooglePlacesClient;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkerService {

    private final MarkerRepository markerRepository;
    private final PostRepository postRepository;
    private final GooglePlacesClient googlePlacesClient;

    // 권한 체크
    private void validateOwner(Post post, Long memberId) {
        Long ownerId = post.getTrip().getOwner().getId();

        if (!ownerId.equals(memberId)) {
            throw new ServiceException("403-1", "권한이 없습니다.");
        }
    }

    // 생성
    public MarkerResponse createMarker(Long postId, Long memberId, MarkerCreateRequest request) {

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        validateOwner(post, memberId);

        Marker marker = new Marker(
            post,
            request.centerLat(),
            request.centerLng(),
            request.placeName(),
            request.visitedAt(),
            request.source()
        );

        Marker saved = markerRepository.save(marker);

        return new MarkerResponse(saved);
    }

    // 목록
    public List<MarkerResponse> getMarkers(Long postId) {

        postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        return markerRepository.findByPostId(postId)
            .stream()
            .map(MarkerResponse::new)
            .toList();
    }

    // 상세
    public MarkerResponse getMarker(Long markerId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        return new MarkerResponse(marker);
    }

    // 장소명 후보 조회
    public List<PlaceCandidateResponse> getPlaceCandidates(Long markerId, Long memberId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        validateOwner(marker.getPost(), memberId);

        // 자동 생성 때는 지역명만 저장하고, 사용자가 수정 화면에서 펼칠 때만 주변 상호명을 조회한다.
        return googlePlacesClient.findNearbyPlaces(marker.getCenterLat(), marker.getCenterLng());
    }

    public List<PlaceCandidateResponse> searchPlaces(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new ServiceException("400-1", "검색어를 입력해주세요.");
        }

        return googlePlacesClient.searchPlaces(keyword);
    }

    public List<PlaceCandidateResponse> findNearbyPlaces(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new ServiceException("400-1", "좌표를 입력해주세요.");
        }

        return googlePlacesClient.findNearbyPlaces(latitude, longitude);
    }

    // 수정
    @Transactional
    public MarkerResponse modifyMarker(Long markerId, Long memberId, MarkerModifyRequest request) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        validateOwner(marker.getPost(), memberId);

        marker.modify(
            request.centerLat(),
            request.centerLng(),
            request.placeName(),
            request.visitedAt(),
            request.source()
        );

        return new MarkerResponse(marker);
    }

    // 삭제
    public void deleteMarker(Long markerId, Long memberId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        validateOwner(marker.getPost(), memberId);

        markerRepository.delete(marker);
    }
}
