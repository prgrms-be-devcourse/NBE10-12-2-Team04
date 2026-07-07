package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final MarkerRepository markerRepository;
    private final TripLikeRepository tripLikeRepository;

    @Transactional
    public TripResponse create(Long ownerId, TripCreateRequest request) {
        Member owner = memberRepository.findById(ownerId).orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        Trip trip = tripRepository.save(new Trip(
            owner,
            request.title(),
            request.country(),
            request.city(),
            request.startDate(),
            request.endDate(),
            request.visibility()
        ));

        return toResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findTripsByOwnerId(Long ownerId) {
        return tripRepository.findByOwnerId(ownerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> findTripsByOwnerId(Long ownerId, Pageable pageable) {
        return tripRepository.findByOwnerIdOrderByCreatedAtDescIdDesc(ownerId, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findPublicTrips() {
        return tripRepository.findByVisibilityTrue()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findAccessibleTrip(Long tripId, Long ownerId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));

        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId);
        }

        return toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Trip findOwnedTrip(Long tripId, Long ownerId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));
        // 이미지 업로드처럼 여행기 내부 데이터를 변경하는 로직은 공개 여부와 무관하게 소유자만 접근 가능
        validateOwner(trip, ownerId);

        return trip;
    }

    @Transactional
    public TripResponse modifyTrip(Long tripId, Long ownerId, TripModifyRequest request) {
        Trip trip = findOwnedTrip(tripId, ownerId);

        trip.modify(
            request.title(),
            request.country(),
            request.city(),
            request.startDate(),
            request.endDate(),
            request.visibility()
        );

        return toResponse(trip);
    }

    @Transactional
    public void deleteTrip(Long tripId, Long ownerId) {
        Trip trip = findOwnedTrip(tripId, ownerId);
        List<Post> posts = postRepository.findByTripId(tripId);
        List<Long> postIds = posts.stream()
            .map(Post::getId)
            .toList();

        trip.changeRepresentativeImage(null);

        if (!postIds.isEmpty()) {
            markerRepository.deleteAll(markerRepository.findByPostIdIn(postIds));
        }

        tripLikeRepository.deleteByTripId(tripId);
        imageRepository.deleteAll(imageRepository.findByTripId(tripId));
        postRepository.deleteAll(posts);
        tripRepository.delete(trip);
    }

    @Transactional
    public TripResponse changeRepresentativeImage(Long tripId, Long ownerId, Long imageId) {
        Trip trip = findOwnedTrip(tripId, ownerId);
        Image image = imageRepository.findById(imageId)
            .orElseThrow(() -> new ServiceException("404-1", "이미지를 찾을 수 없습니다."));

        if (!image.getTrip().getId().equals(trip.getId()) || !image.getOwner().getId().equals(ownerId)) {
            throw new ServiceException("403-1", "이미지에 대한 권한이 없습니다.");
        }

        trip.changeRepresentativeImage(image);

        return toResponse(trip);
    }

    // 공개여행기 중 최근 한 달 좋아요 수 상위 10개 조회
    @Transactional(readOnly = true)
    public List<TripResponse> findTop10PublicTripsByLikeCount() {
        LocalDateTime likedSince = LocalDateTime.now().minusMonths(1);

        return tripRepository.findTop10PublicTripsByRecentLikeCount(likedSince)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // 공개여행기 중 createdAt 기준 내림차순 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<TripResponse> findPublicTripsByCreatedAtDesc() {
        return tripRepository.findByVisibilityTrueOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> findPublicTripsByCreatedAtDesc(Pageable pageable) {
        return tripRepository.findByVisibilityTrueOrderByCreatedAtDescIdDesc(pageable)
            .map(this::toResponse);
    }

    private TripResponse toResponse(Trip trip) {
        return new TripResponse(trip);
    }

    private void validateOwner(Trip trip, Long ownerId) {
        if (!trip.getOwner().getId().equals(ownerId)) {
            throw new ServiceException("403-1", "여행기에 대한 권한이 없습니다.");
        }
    }
}
