package com.triptrace.domain.image.image.repository;

import com.triptrace.domain.image.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByTripId(Long tripId);

    List<Image> findByPostId(Long postId);

    List<Image> findByMarkerId(Long markerId);

    List<Image> findByOwnerId(Long ownerId);
}
