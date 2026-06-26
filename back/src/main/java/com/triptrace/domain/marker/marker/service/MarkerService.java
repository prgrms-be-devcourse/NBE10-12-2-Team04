package com.triptrace.domain.marker.marker.service;

import com.triptrace.domain.marker.marker.dto.*;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.global.rsData.RsData;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkerService {

    private final MarkerRepository markerRepository;
    private final PostRepository postRepository;

    // 권한 체크
    private void validateOwner(Post post, Long memberId) {
        Long ownerId = post.getTrip().getOwner().getId();

        if (!ownerId.equals(memberId)) {
            throw new ServiceException("403-1", "권한이 없습니다.");
        }
    }

    // 생성
    public RsData<MarkerResponse> createMarker(Long postId, MarkerCreateReqBody reqBody) {

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        validateOwner(post, reqBody.memberId());

        Marker marker = new Marker(
            post,
            reqBody.centerLat(),
            reqBody.centerLng(),
            reqBody.placeName(),
            reqBody.visitedAt(),
            reqBody.source()
        );

        Marker saved = markerRepository.save(marker);

        return new RsData<>("201-1", "마커가 생성되었습니다.", new MarkerResponse(saved));
    }

    // 목록
    public RsData<List<MarkerResponse>> getMarkers(Long postId) {

        postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        List<Marker> markers = markerRepository.findAllByPostId(postId);

        List<MarkerResponse> result = markers.stream()
            .map(MarkerResponse::new)
            .toList();

        return new RsData<>("200-1", "마커 목록 조회 성공", result);
    }

    // 상세
    public RsData<MarkerResponse> getMarker(Long markerId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        return new RsData<>("200-1", "마커 조회 성공", new MarkerResponse(marker));
    }

    // 수정
    public RsData<MarkerResponse> modifyMarker(Long markerId, MarkerModifyReqBody reqBody) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        validateOwner(marker.getPost(), reqBody.memberId());

        marker.modify(
            reqBody.centerLat(),
            reqBody.centerLng(),
            reqBody.placeName(),
            reqBody.visitedAt(),
            reqBody.source()
        );

        return new RsData<>("200-1", "마커가 수정되었습니다.", new MarkerResponse(marker));
    }

    // 삭제
    public RsData<Void> deleteMarker(Long markerId, Long memberId) {

        Marker marker = markerRepository.findById(markerId)
            .orElseThrow(() -> new ServiceException("404-1", "마커를 찾을 수 없습니다."));

        validateOwner(marker.getPost(), memberId);

        markerRepository.delete(marker);

        return new RsData<>("200-1", "마커가 삭제되었습니다.", null);
    }
}
