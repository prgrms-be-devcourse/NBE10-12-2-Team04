package com.triptrace.domain.post.post.dto;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.post.post.entity.Post;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
    Long id,
    Long tripId,
    LocalDate date,
    String title,
    String memo,
    List<PostImageResponse> images,
    PostMarkerResponse marker,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public PostResponse(Post post) {
        this(post, List.of(), null);
    }

    public PostResponse(Post post, List<Image> images, Marker marker) {
        this(
            post.getId(),
            post.getTrip().getId(),
            post.getDate(),
            post.getTitle(),
            post.getMemo(),
            images.stream()
                .map(PostImageResponse::new)
                .toList(),
            marker == null ? null : new PostMarkerResponse(marker),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }

    public record PostImageResponse(
        Long id,
        String originalFileUrl,
        String thumbnailUrl,
        String mimeType,
        LocalDateTime capturedAt
    ) {
        public PostImageResponse(Image image) {
            this(
                image.getId(),
                image.getOriginalFileUrl(),
                image.getThumbnailUrl(),
                image.getMimeType(),
                image.getCapturedAt()
            );
        }
    }

    public record PostMarkerResponse(
        Long id,
        Long postId,
        BigDecimal centerLat,
        BigDecimal centerLng,
        String placeName,
        LocalDateTime visitedAt,
        String source,
        Long representativeImageId,
        String representativeThumbnailUrl
    ) {
        public PostMarkerResponse(Marker marker) {
            this(
                marker.getId(),
                marker.getPost().getId(),
                marker.getCenterLat(),
                marker.getCenterLng(),
                marker.getPlaceName(),
                marker.getVisitedAt(),
                marker.getSource().name(),
                marker.getRepresentativeImage() == null ? null : marker.getRepresentativeImage().getId(),
                marker.getRepresentativeImage() == null
                    ? null : marker.getRepresentativeImage().getThumbnailUrl()
            );
        }
    }
}
