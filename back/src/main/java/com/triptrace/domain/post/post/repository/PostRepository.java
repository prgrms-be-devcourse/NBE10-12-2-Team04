package com.triptrace.domain.post.post.repository;

import com.triptrace.domain.post.post.dto.PostResponse;
import com.triptrace.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByTripId(Long tripId);

    List<Post> findByTripIdOrderByDateAsc(Long tripId);

    @Query("select p from Post p join p.trip t join t.owner m where m.id = :ownerId order by p.date asc, p.id asc")
	List<Post> findByOwnerId(@Param("ownerId") Long ownerId);
}
