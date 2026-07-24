package com.triptrace.domain.image.image.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.triptrace.domain.image.image.dto.response.ImageResponse;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.processing.ImageInfo;
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo;
import com.triptrace.domain.image.image.storage.StoredImageFile;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;

public class ImageMapper {
    private static final int PUBLIC_GPS_SCALE = 7;

    public static Image toEntity(Member owner, Trip trip, ImageInfo imageInfo, StoredImageFile storedImageFile) {
        return toEntity(
            owner,
            trip,
            null,
            imageInfo,
            storedImageFile
        );
    }

    public static ImageServiceResponse toServiceResponse(Image image) {
        Long postId = null;
        Post post = image.getPost();
        if (post != null) {
            postId = post.getId();
        }
        return new ImageServiceResponse(
            image.getId(),
            image.getOwner().getId(),
            image.getTrip().getId(),
            postId,
            image.getOriginalFileUrl(),
            image.getThumbnailUrl(),
            image.getMimeType(),
            toPublicGps(image.getGpsLat()),
            toPublicGps(image.getGpsLng()),
            image.getCapturedAt(),
            image.getDeviceInfo(),
            image.getUploadStatus()
        );
    }

    public static StoredImageFile toStoredImageFile(String imageFileUrl, String thumbnailImageFileUrl,
        Long fileSize, String mimeType) {
        return new StoredImageFile(imageFileUrl, thumbnailImageFileUrl, fileSize, mimeType);
    }

    public static StoredImageFile toStoredImageFile(SavedFileInfo savedFileInfo) {
        return toStoredImageFile(
            savedFileInfo.servingUrl(),
            savedFileInfo.thumbnailUrl(),
            savedFileInfo.size(),
            savedFileInfo.mimeType());
    }

    public static ImageUploadResponse toUploadResponse(String fileName,
        ImageServiceResponse imageServiceResponse, String message) {
        if (imageServiceResponse == null) {
            return new ImageUploadResponse(fileName, null, null, null, null, UploadStatus.FAILED, message);
        }
        return new ImageUploadResponse(
            fileName,
            imageServiceResponse.id(),
            imageServiceResponse.originalFileUrl(),
            imageServiceResponse.thumbnailUrl(),
            imageServiceResponse.mimeType(),
            imageServiceResponse.uploadStatus(),
            "SUCCESS"
        );
    }

    public static ImageUploadResponse toUploadResponse(String fileName,
        ImageServiceResponse imageServiceResponse) {
        return toUploadResponse(fileName, imageServiceResponse, "ERROR");
    }

    public static Image toEntity(Member owner, Trip trip, Post post, ImageInfo imageInfo,
        StoredImageFile storedImageFile) {
        UploadStatus uploadStatus = UploadStatus.STORED;
        if (storedImageFile == null ||
            storedImageFile.imageFileUrl() == null ||
            storedImageFile.imageFileUrl().isBlank()) {
            uploadStatus = UploadStatus.FAILED;
        }
        String device = null;
        if (imageInfo.getMaker() != null && imageInfo.getModel() != null) {
            device = "%s - %s".formatted(imageInfo.getMaker(), imageInfo.getModel());
        }
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if (imageInfo.getLatitude() != null && imageInfo.getLongitude() != null) {
            latitude = BigDecimal.valueOf(imageInfo.getLatitude());
            longitude = BigDecimal.valueOf(imageInfo.getLongitude());
        }
        return new Image(
            owner,
            trip,
            post,
            storedImageFile.imageFileUrl(),
            storedImageFile.thumbnailImageFileUrl(),
            storedImageFile.fileSize(),
            storedImageFile.mimeType(),
            latitude,
            longitude,
            imageInfo.getCapturedAt(),
            device,
            uploadStatus
        );
    }

    private static BigDecimal toPublicGps(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(PUBLIC_GPS_SCALE, RoundingMode.FLOOR);
    }

	public static ImageResponse toImageResponse(ImageServiceResponse imageServiceResponse) {
        return new ImageResponse(imageServiceResponse.id(),
            imageServiceResponse.ownerId(),
            imageServiceResponse.tripId(),
            imageServiceResponse.postId(),
            imageServiceResponse.originalFileUrl(),
            imageServiceResponse.thumbnailUrl());
	}
}
