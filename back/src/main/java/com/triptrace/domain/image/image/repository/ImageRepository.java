package com.triptrace.domain.image.image.repository;

import com.triptrace.domain.image.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByTripId(Long tripId);

    List<Image> findByPostId(Long postId);

    List<Image> findByPostIdIn(List<Long> postIds);

    List<Image> findByOwnerId(Long ownerId);

    Optional<Image> findByOriginalFileUrl(String originalFileUrl);
}
