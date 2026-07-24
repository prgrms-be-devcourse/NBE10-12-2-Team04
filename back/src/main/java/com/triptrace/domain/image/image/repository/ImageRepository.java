package com.triptrace.domain.image.image.repository;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.trip.trip.entity.Trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByTripId(Long tripId);

    List<Image> findByPostId(Long postId);

    List<Image> findByPostIdIn(List<Long> postIds);

    List<Image> findByOwnerId(Long ownerId);

    Optional<Image> findByOriginalFileUrl(String originalFileUrl);

    List<Image> trip(Trip trip);

    @Query("""
        select i from Image i
        where i.id =:imageId
        and i.owner.id =:ownerId
        and i.trip.id =:tripId
    """)
    Optional<Image> findByIdAndOwnerIdAndTripId(@Param("imageId")Long id,@Param("ownerId") Long OwnerId,@Param("tripId") Long TripId);
}
