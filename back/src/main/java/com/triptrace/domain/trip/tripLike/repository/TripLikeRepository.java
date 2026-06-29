package com.triptrace.domain.trip.tripLike.repository;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.trip.tripLike.entity.TripLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripLikeRepository extends JpaRepository<TripLike, Long> {

    boolean existsByMemberIdAndTripId(Long memberId, Long tripId);

    Optional<TripLike> findByMemberIdAndTripId(Long memberId, Long tripId);

    long countByTripId(Long tripId);

    Long member(Member member);
}
