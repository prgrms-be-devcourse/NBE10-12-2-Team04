package com.triptrace.domain.post.post.repository;

import com.triptrace.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByTripId(Long tripId);

    List<Post> findByTripIdOrderByDateAsc(Long tripId);
}
