package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.catalog.ImageExceptionCatalog;
import com.triptrace.domain.image.image.dto.ImageFileRequest;
import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.factory.ImageFactory;
import com.triptrace.domain.image.image.module.ImageInfo;
import com.triptrace.domain.image.image.module.ImageMetadataExtractor;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import com.triptrace.domain.image.image.module.storage.ImageFileStorage;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadFacade {
    private final ImageService imageService;
    private final ImageMetadataExtractor imageMetadataExtractor;
    private final ImageFileStorage imageFileStorage;

    private final TripService tripService;
    private final MemberService memberService;
    private final PostService postService;

    private ImageInfo extract(MultipartFile imageFile) {
        ImageInfo imageInfo = new ImageInfo();
        try{
            byte[] bytes = imageFile.getBytes();
            imageInfo = imageMetadataExtractor.extract(bytes);
        }
        catch(IOException | ImageProcessException e){
            //추출단계는 권장 영역이므로 예외를 올리지 않고 로그만 기록
            log.warn(e.getMessage());
        }
        return imageInfo;
    }

    ImageUploadResponse upload(Member owner, Trip trip, MultipartFile imageFile) {
        return upload(owner, trip, null, imageFile);
    }
    ImageUploadResponse upload(Member owner, Trip trip, Post post, MultipartFile imageFile) {
        //예외 반환 안 함
        //uploadImages의 부속 메서드 10개 중 2개 실패한다더라도 8개는 저장해야함
        String fileName = imageFile.getOriginalFilename();
        SavedFileInfo savedFileInfo;
        ImageFileRequest imageFileRequest;
        //extract
        ImageInfo imageInfo = extract(imageFile);
        try {
            byte[] bytes = imageFile.getBytes();
            savedFileInfo = imageFileStorage.saveImageWithThumbnail(bytes, imageInfo.getOrientation());
            if(savedFileInfo == null){
                throw ImageExceptionCatalog.invalid("이미지 파일 저장에 실패했습니다.");
            }
            imageFileRequest = ImageFactory.createImageFileRequest(savedFileInfo);
        }
        catch(IOException | ImageProcessException e){
            //저장 실패시 응답 반환 - 로그만
            log.warn(e.getMessage());
            return ImageFactory.createImageUploadResponse(fileName, null);
        }
        //insert db
        Image image = ImageFactory.createImage(owner, trip, post, imageInfo, imageFileRequest);
        ImageServiceResponse imageServiceResponse = null;
        try {
            imageServiceResponse = imageService.create(image);
        }catch(IllegalArgumentException | OptimisticLockingFailureException e){
            //DB 저장 실패
            imageFileStorage.cleanUp(savedFileInfo);
            log.warn(e.getMessage());
        }
        return ImageFactory.createImageUploadResponse(fileName, imageServiceResponse);
    }

    public List<ImageUploadResponse> uploadImages(Long ownerId,
                                                  Long tripId,
                                                  @NotEmpty MultipartFile[] images){
        //다수의 파일 업로드
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
        //Post가 있는 경우 업로드
        List<ImageUploadResponse> list = new ArrayList<>();
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        for (MultipartFile imageFile : images) {
            list.add(upload(owner, trip, post, imageFile));
        }
        return list;
    }

    public String uploadProfile(
        @NotEmpty Long ownerId,
        @NotNull MultipartFile imageFile) {
        //애매함
        Member owner = memberService.findById(ownerId);
        String url = null;
        try{
            url = imageFileStorage.saveProfileImage(imageFile.getBytes());
            memberService.modifyProfileImageUrl(ownerId, url);
        }catch (IOException e){
          throw ImageExceptionCatalog.invalid();
        }catch(RuntimeException e){
            if(url != null) imageFileStorage.deleteImage(url);
            throw e;
        }
        if(url == null){
            throw ImageExceptionCatalog.invalid("프로필 이미지 저장에 실패했습니다.");
        }
        return url;
    }


}
