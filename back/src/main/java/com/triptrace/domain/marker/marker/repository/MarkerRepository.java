package com.triptrace.domain.marker.marker.repository;

import com.triptrace.domain.marker.marker.entity.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkerRepository extends JpaRepository<Marker, Long> {

    Optional<Marker> findByPostId(Long postId);

    List<Marker> findByPostIdIn(List<Long> postIds);

    List<Marker> findByRepresentativeImageId(Long representativeImageId);
}
