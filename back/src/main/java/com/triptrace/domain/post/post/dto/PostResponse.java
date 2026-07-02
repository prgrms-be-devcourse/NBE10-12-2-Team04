package com.triptrace.domain.post.post.dto;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.support.ImageUrlResolver;
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
        this(post, List.of(), null, null);
    }

    public PostResponse(Post post, List<Image> images, Marker marker, ImageUrlResolver imageUrlResolver) {
        this(
            post.getId(),
            post.getTrip().getId(),
            post.getDate(),
            post.getTitle(),
            post.getMemo(),
            images.stream()
                .map(image -> new PostImageResponse(image, imageUrlResolver))
                .toList(),
            marker == null ? null : new PostMarkerResponse(marker, imageUrlResolver),
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
        public PostImageResponse(Image image, ImageUrlResolver imageUrlResolver) {
            this(
                image.getId(),
                imageUrlResolver == null ? image.getOriginalFileUrl() : imageUrlResolver.toPublicUrl(image.getOriginalFileUrl()),
                imageUrlResolver == null ? image.getThumbnailUrl() : imageUrlResolver.toPublicUrl(image.getThumbnailUrl()),
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
        public PostMarkerResponse(Marker marker, ImageUrlResolver imageUrlResolver) {
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
                    ? null
                    : imageUrlResolver == null
                        ? marker.getRepresentativeImage().getThumbnailUrl()
                        : imageUrlResolver.toPublicUrl(marker.getRepresentativeImage().getThumbnailUrl())
            );
        }
    }
}
