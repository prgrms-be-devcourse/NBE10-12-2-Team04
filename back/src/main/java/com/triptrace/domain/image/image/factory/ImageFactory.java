package com.triptrace.domain.image.image.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.triptrace.domain.image.image.dto.ImageFileRequest;
import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.module.ImageInfo;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;

public class ImageFactory {
    public static Image createImage(Member owner, Trip trip, ImageInfo imageInfo, ImageFileRequest imageFileRequest) {
        return createImage(
            owner,
            trip,
            null,
            imageInfo,
            imageFileRequest
        );
    }

    public static ImageServiceResponse createImageServiceResponse(Image image) {
        //gps값 정확도 희석
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if (image.getGpsLat() != null && image.getGpsLng() != null) {
            latitude = image.getGpsLat().setScale(4, RoundingMode.FLOOR);
            longitude = image.getGpsLng().setScale(4, RoundingMode.FLOOR);
        }
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
            latitude,
            longitude,
            image.getCapturedAt(),
            image.getDeviceInfo(),
            image.getUploadStatus()
        );
    }

    public static ImageFileRequest createImageFileRequest(String imageFileUrl, String thumbnailImageFileUrl,
        Long fileSize, String mimeType) {
        return new ImageFileRequest(imageFileUrl, thumbnailImageFileUrl, fileSize, mimeType);
    }

    public static ImageFileRequest createImageFileRequest(SavedFileInfo savedFileInfo) {
        return createImageFileRequest(
            savedFileInfo.servingUrl(),
            savedFileInfo.thumbnailUrl(),
            savedFileInfo.size(),
            savedFileInfo.mimeType());
    }

    public static ImageUploadResponse createImageUploadResponse(String fileName,
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

    public static ImageUploadResponse createImageUploadResponse(String fileName,
        ImageServiceResponse imageServiceResponse) {
        return createImageUploadResponse(fileName, imageServiceResponse, "ERROR");
    }

    public static Image createImage(Member owner, Trip trip, Post post, ImageInfo imageInfo,
        ImageFileRequest imageFileRequest) {
        UploadStatus uploadStatus = UploadStatus.STORED;
        if (imageFileRequest == null ||
            imageFileRequest.imageFileUrl() == null ||
            imageFileRequest.imageFileUrl().isBlank()) {
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
            imageFileRequest.imageFileUrl(),
            imageFileRequest.thumbnailImageFileUrl(),
            imageFileRequest.fileSize(),
            imageFileRequest.mimeType(),
            latitude,
            longitude,
            imageInfo.getCapturedAt(),
            device,
            uploadStatus
        );
    }
}
