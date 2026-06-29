package com.triptrace.domain.trip.trip.service;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.dto.TripCreateReqBody;
import com.triptrace.domain.trip.trip.dto.TripModifyReqBody;
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
    public TripResponse create(TripCreateReqBody reqBody) {
        Member owner = memberRepository.findById(reqBody.memberId()).orElseThrow();

        Trip trip = tripRepository.save(new Trip(
            owner,
            reqBody.title(),
            reqBody.country(),
            reqBody.city(),
            reqBody.startDate(),
            reqBody.endDate(),
            reqBody.visibility()
        ));

        return new TripResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findMine(Long memberId) {
        return tripRepository.findByOwnerId(memberId)
            .stream()
            .map(TripResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(Long tripId) {
        return new TripResponse(getTrip(tripId));
    }

    @Transactional
    public TripResponse modify(Long tripId, TripModifyReqBody reqBody) {
        Trip trip = getTrip(tripId);
        validateOwner(trip, reqBody.memberId());

        trip.modify(
            reqBody.title(),
            reqBody.country(),
            reqBody.city(),
            reqBody.startDate(),
            reqBody.endDate(),
            reqBody.visibility()
        );

        return new TripResponse(trip);
    }

    @Transactional
    public void delete(Long tripId, Long memberId) {
        Trip trip = getTrip(tripId);
        validateOwner(trip, memberId);

        tripRepository.delete(trip);
    }

    private Trip getTrip(Long tripId) {
        return tripRepository.findById(tripId).orElseThrow();
    }

    private void validateOwner(Trip trip, Long memberId) {
        if (!trip.getOwner().getId().equals(memberId)) {
            throw new ServiceException("403-1", "여행기에 대한 권한이 없습니다.");
        }
    }
}
