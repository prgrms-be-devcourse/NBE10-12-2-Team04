package com.triptrace.domain.image.image.dto.response;

public record ImageResponse (
    Long id,
    Long ownerId,
    Long tripId,
    Long postId,
    String originalUrl,
    String thumbnailUrl){
}
