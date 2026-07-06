package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.trip.trip.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByOwnerId(Long ownerId);

    Page<Trip> findByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId, Pageable pageable);

    List<Trip> findByVisibilityTrue();

    List<Trip> findByRepresentativeImageId(Long representativeImageId);

    Page<Trip> findByVisibilityTrueOrderByCreatedAtDescIdDesc(Pageable pageable);

    // 공개여행기 중 좋아요 수 상위 10개 조회 쿼리 추가
    List<Trip> findTop10ByVisibilityTrueOrderByLikeCountDesc();

    // 공개여행기 중 createdAt 기준 내림차순 조회 퀴리 추가
    List<Trip> findByVisibilityTrueOrderByCreatedAtDesc();
}
