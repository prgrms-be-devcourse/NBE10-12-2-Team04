package com.triptrace.domain.image.image.factory;

import com.triptrace.domain.image.image.dto.ImageFileRequest;
import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.entity.UploadStatus;
import com.triptrace.domain.image.image.module.ImageInfo;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.trip.trip.entity.Trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class ImageFactory {
    public static Image createImage(Member owner, Trip trip, ImageInfo imageInfo, ImageFileRequest imageFileRequest) {
        UploadStatus uploadStatus = UploadStatus.STORED;
        if(imageFileRequest == null ||
            imageFileRequest.getImageFileUrl() == null ||
            imageFileRequest.getImageFileUrl().isBlank()){
            uploadStatus = UploadStatus.FAILED;
        }
        //서버 시스템 기준으로 시간 변환
        LocalDateTime capturedAt = null;
        if(imageInfo.getCapturedAt() != null){
            capturedAt = imageInfo.getCapturedAt();
        }
        String device = null;
        if(imageInfo.getMaker()!=null && imageInfo.getModel()!=null){
            device = "%s - %s".formatted(imageInfo.getMaker(), imageInfo.getModel());
        }
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if(imageInfo.getLatitude() != null && imageInfo.getLongitude() != null){
            latitude =  BigDecimal.valueOf(imageInfo.getLatitude());
            longitude = BigDecimal.valueOf(imageInfo.getLongitude());
        }

        return new Image(
            owner,
            trip,
            null,
            imageFileRequest.getImageFileUrl(),
            imageFileRequest.getThumbnailImageFileUrl(),
            imageFileRequest.getFileSize(),
            imageInfo.getMimeType(),
            latitude,
            longitude,
            capturedAt,
            device,
            uploadStatus
        );
    }
    public static ImageServiceResponse createImageServiceResponse(Image image){
        //gps값 정확도 희석
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if(image.getGpsLat() != null && image.getGpsLng() != null) {
            latitude = image.getGpsLat().setScale(4, RoundingMode.FLOOR);
            longitude = image.getGpsLng().setScale(4, RoundingMode.FLOOR);
        }
        return new ImageServiceResponse(
            image.getId(),
            image.getOwner(),
            image.getTrip(),
            image.getPost(),
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
    public static ImageFileRequest createImageFileRequest(String imageFileUrl,String thumbnailImageFileUrl, Long fileSize) {
        return new ImageFileRequest(imageFileUrl, thumbnailImageFileUrl, fileSize);
    }
    public static ImageFileRequest createImageFileRequest(List<SavedFileInfo> fileList) {
        SavedFileInfo savedFileInfoOrigin = fileList.getFirst();
        SavedFileInfo savedFileInfoThumbnail = fileList.getLast();
        return createImageFileRequest(
            savedFileInfoOrigin.path(),
            savedFileInfoThumbnail.path(),
            savedFileInfoOrigin.size());
    }
    public static ImageUploadResponse createImageUploadResponse(String fileName, ImageServiceResponse imageServiceResponse) {
        if(imageServiceResponse == null){
            return new ImageUploadResponse(fileName,null,null,null,null,UploadStatus.FAILED);
        }
        return new ImageUploadResponse(
            fileName,
            imageServiceResponse.id(),
            imageServiceResponse.originalFileUrl(),
            imageServiceResponse.thumbnailUrl(),
            imageServiceResponse.mimeType(),
            imageServiceResponse.uploadStatus()
        );
    }
}
