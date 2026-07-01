package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.dto.TripCreateRequest;
import com.triptrace.domain.trip.trip.dto.TripModifyRequest;
import com.triptrace.domain.trip.trip.dto.TripResponse;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final MemberRepository memberRepository;

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

        return new TripResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findTripsByOwnerId(Long ownerId) {
        return tripRepository.findByOwnerId(ownerId)
            .stream()
            .map(TripResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findPublicTrips() {
        return tripRepository.findByVisibilityTrue()
            .stream()
            .map(TripResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findAccessibleTrip(Long tripId, Long ownerId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));

        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId);
        }

        return new TripResponse(trip);
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

        return new TripResponse(trip);
    }

    @Transactional
    public void deleteTrip(Long tripId, Long ownerId) {
        Trip trip = findOwnedTrip(tripId, ownerId);

        tripRepository.delete(trip);
    }

    // 공개여행기 중 좋아요 수 상위 10개 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<TripResponse> findTop10PublicTripsByLikeCount() {
        return tripRepository.findTop10ByVisibilityTrueOrderByLikeCountDesc()
            .stream()
            .map(TripResponse::new)
            .toList();
    }

    // 공개여행기 중 createdAt 기준 내림차순 조회 메서드 추가
    @Transactional(readOnly = true)
    public List<TripResponse> findPublicTripsByCreatedAtDesc() {
        return tripRepository.findByVisibilityTrueOrderByCreatedAtDesc()
            .stream()
            .map(TripResponse::new)
            .toList();
    }


    private void validateOwner(Trip trip, Long ownerId) {
        if (!trip.getOwner().getId().equals(ownerId)) {
            throw new ServiceException("403-1", "여행기에 대한 권한이 없습니다.");
        }
    }
}


