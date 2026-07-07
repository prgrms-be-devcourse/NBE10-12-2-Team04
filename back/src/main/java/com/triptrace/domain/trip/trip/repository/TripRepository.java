package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.trip.trip.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByOwnerId(Long ownerId);

    Page<Trip> findByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId, Pageable pageable);

    List<Trip> findByVisibilityTrue();

    List<Trip> findByRepresentativeImageId(Long representativeImageId);

    Page<Trip> findByVisibilityTrueOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
        SELECT tl.trip
        FROM TripLike tl
        WHERE tl.trip.visibility = true
            AND tl.createdAt >= :likedSince
        GROUP BY tl.trip
        ORDER BY COUNT(tl.id) DESC, MAX(tl.trip.createdAt) DESC, MAX(tl.trip.id) DESC
        """)
    List<Trip> findTop10PublicTripsByRecentLikeCount(@Param("likedSince") LocalDateTime likedSince);

    // 공개여행기 중 createdAt 기준 내림차순 조회 퀴리 추가
    List<Trip> findByVisibilityTrueOrderByCreatedAtDesc();
}
