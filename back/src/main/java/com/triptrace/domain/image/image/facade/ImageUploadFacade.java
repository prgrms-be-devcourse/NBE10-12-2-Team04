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
import com.triptrace.global.exception.ServiceException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    //member service로 대체
    private final MemberService memberService;
    private final PostService postService;

    private ImageInfo extract(MultipartFile imageFile) {
        ImageInfo imageInfo = new ImageInfo();
        try{//extract
            byte[] bytes = imageFile.getBytes();
            imageInfo = imageMetadataExtractor.extract(bytes);
        }
        catch(IOException | ServiceException | ImageProcessException e){
            log.warn(ImageExceptionCatalog.invalid("추출 실패").toString());
            log.warn(e.getMessage());
        }
        return imageInfo;
    }
    ImageUploadResponse upload(Long ownerId, Long tripId, MultipartFile imageFile) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        String fileName = imageFile.getOriginalFilename();
        SavedFileInfo savedFileInfo;
        ImageFileRequest imageFileRequest;

        ImageInfo imageInfo = extract(imageFile);
        try { //image file save
            byte[] bytes = imageFile.getBytes();
            savedFileInfo = imageFileStorage.saveImageWithThumbnail(bytes, imageInfo.getOrientation());
            if(savedFileInfo == null){
                throw ImageExceptionCatalog.invalid("저장 오류");
            }
            imageFileRequest = ImageFactory.createImageFileRequest(savedFileInfo);
        }
        catch(IOException | ServiceException | ImageProcessException e){
            //저장 실패시 응답 반환
            log.warn(ImageExceptionCatalog.invalid().toString());
            log.warn(e.getMessage());
            return ImageFactory.createImageUploadResponse(fileName, null);
        }
        //insert db
        Image image = ImageFactory.createImage(owner, trip, imageInfo, imageFileRequest);
        ImageServiceResponse imageServiceResponse = imageService.create(image);
        return ImageFactory.createImageUploadResponse(fileName, imageServiceResponse);
    }
    public List<ImageUploadResponse> uploadImages(Long ownerId,
                                                  Long tripId,
                                                  @NotEmpty MultipartFile[] images){
        List<ImageUploadResponse> list = new ArrayList<>();
        for (MultipartFile image : images) {
            list.add(upload(ownerId, tripId, image));
        }
        return list;
    }

    public ImageUploadResponse uploadImage(Long ownerId, Long tripId, Long postId, MultipartFile imageFile) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        ImageInfo imageInfo = extract(imageFile);
        SavedFileInfo savedFileInfo;
        try {
            byte[] bytes = imageFile.getBytes();
            savedFileInfo = imageFileStorage.saveImageWithThumbnail(bytes,imageInfo.getOrientation());
            if(savedFileInfo == null){
                throw ImageExceptionCatalog.invalid();
            }
        } catch (IOException | ServiceException e) {
            throw ImageExceptionCatalog.invalid();
        }
        Image image = ImageFactory.createImage(owner, trip, post, imageInfo, ImageFactory.createImageFileRequest(savedFileInfo));
        return ImageFactory.createImageUploadResponse(imageFile.getName(),imageService.create(image));
    }

    public String uploadProfile(
        @NotEmpty Long ownerId,
        @NotNull MultipartFile imageFile) {
        Member owner = memberService.findById(ownerId);
        String url;
        try{
            url = imageFileStorage.saveProfileImage(imageFile.getBytes());
            memberService.modifyProfileImageUrl(ownerId, url);
        }catch (IOException | ServiceException e){
          throw ImageExceptionCatalog.invalid();
        }
        if(url == null){
            throw ImageExceptionCatalog.invalid("프로필 이미지 저장에 실패했습니다.");
        }
        return url;
    }
}
