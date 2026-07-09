package com.triptrace.domain.image.image.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.triptrace.domain.image.image.catalog.ImageExceptionCatalog;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.response.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.mapper.ImageMapper;
import com.triptrace.domain.image.image.processing.ImageInfo;
import com.triptrace.domain.image.image.processing.ImageMetadataExtractor;
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo;
import com.triptrace.domain.image.image.processing.exception.ImageProcessException;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.image.image.storage.ImageFileStorage;
import com.triptrace.domain.image.image.storage.StoredImageFile;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;

import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadUseCase {
    private final ImageService imageService;
    private final ImageMetadataExtractor imageMetadataExtractor;
    private final ImageFileStorage imageFileStorage;

    private final TripService tripService;
    private final MemberService memberService;
    private final PostService postService;

    private ImageInfo extract(MultipartFile imageFile) {
        try {
            byte[] bytes = imageFile.getBytes();
            return imageMetadataExtractor.extract(bytes);
        } catch (IOException | ImageProcessException e) {
            log.warn(e.getMessage());
            return new ImageInfo();
        }
    }

    private ImageUploadResponse upload(Member owner, Trip trip, MultipartFile imageFile) {
        return upload(owner, trip, null, imageFile);
    }

    private ImageUploadResponse upload(Member owner, Trip trip, Post post, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return ImageMapper.toUploadResponse(null, null, "EMPTY_FILE");
        }
        String fileName = imageFile.getOriginalFilename();
        SavedFileInfo savedFileInfo;
        StoredImageFile storedImageFile;
        ImageInfo imageInfo = extract(imageFile);
        try {
            byte[] bytes = imageFile.getBytes();
            savedFileInfo = imageFileStorage.saveImageWithThumbnail(bytes, imageInfo.getOrientation());
            if (savedFileInfo == null) {
                throw ImageExceptionCatalog.invalid("이미지 파일 저장에 실패했습니다.");
            }
            storedImageFile = ImageMapper.toStoredImageFile(savedFileInfo);
        } catch (IOException | ImageProcessException e) {
            log.warn(e.getMessage());
            return ImageMapper.toUploadResponse(fileName, null, "FILE SAVE FAILED");
        }
        Image image = ImageMapper.toEntity(owner, trip, post, imageInfo, storedImageFile);
        ImageServiceResponse imageServiceResponse;
        try {
            imageServiceResponse = imageService.create(image);
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            imageFileStorage.cleanUp(savedFileInfo);
            log.warn(e.getMessage());
            return ImageMapper.toUploadResponse(fileName, null, "SERVER SAVE FAILED");
        }
        return ImageMapper.toUploadResponse(fileName, imageServiceResponse);
    }

    public List<ImageUploadResponse> uploadImages(Long ownerId,
        Long tripId,
        @NotEmpty MultipartFile[] images) {
        // 다중 업로드는 파일별 부분 성공을 API 계약으로 유지한다.
        validateImagesRequest(images);
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        List<ImageUploadResponse> list = new ArrayList<>();
        for (MultipartFile image : images) {
            list.add(upload(owner, trip, image));
        }
        return list;
    }

    public List<ImageUploadResponse> uploadImages(Long ownerId,
        Long tripId,
        Long postId,
        @NotEmpty MultipartFile[] images) {
        // 다중 업로드는 파일별 부분 성공을 API 계약으로 유지한다.
        validateImagesRequest(images);
        List<ImageUploadResponse> list = new ArrayList<>();
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        for (MultipartFile imageFile : images) {
            list.add(upload(owner, trip, post, imageFile));
        }
        return list;
    }

    private void validateImagesRequest(MultipartFile[] images) {
        if (images == null || images.length == 0) {
            throw ImageExceptionCatalog.invalid("업로드할 이미지가 없습니다.");
        }
    }
}
