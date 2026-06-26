package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.TriptraceApplication;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripLikeService {
    public final TripLikeRepository tripLikeRepository;
    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;

    public void tripLike(Long memberId, Long tripId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Trip trip = tripRepository.findById(tripId).orElseThrow();

        TripLike tripLike = new TripLike(member, trip);

        tripLikeRepository.save(tripLike);
        trip.increaseLikeCount();
    }

    public void tripUnlike(Long memberId, Long tripId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Trip trip = tripRepository.findById(tripId).orElseThrow();

        TripLike tripLike = tripLikeRepository.findByMemberIdAndTripId(memberId, tripId).orElseThrow();

        tripLikeRepository.delete(tripLike);
        trip.decreaseLikeCount();
    }
}
