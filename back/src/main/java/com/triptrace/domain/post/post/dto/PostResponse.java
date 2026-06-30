package com.triptrace.domain.post.post.dto;

import com.triptrace.domain.post.post.entity.Post;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    Long tripId,
    LocalDate date,
    String title,
    String memo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PostResponse(Post post) {
        this(
            post.getId(),
            post.getTrip().getId(),
            post.getDate(),
            post.getTitle(),
            post.getMemo(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
