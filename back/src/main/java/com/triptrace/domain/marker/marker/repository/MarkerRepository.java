package com.triptrace.domain.marker.marker.repository;

import com.triptrace.domain.marker.marker.entity.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarkerRepository extends JpaRepository<Marker, Long> {

    List<Marker> findAllByPostId(Long postId);
}
