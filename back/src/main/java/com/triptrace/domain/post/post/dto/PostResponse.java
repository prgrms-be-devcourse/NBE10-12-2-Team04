package com.triptrace.domain.post.post.dto;

import com.triptrace.domain.post.post.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    Long ownerId,
    String title,
    String country,
    String city,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean visibility,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long likeCount
) {
    public PostResponse(Post post) {
        this(
            post.getId()
        );
    }
}
