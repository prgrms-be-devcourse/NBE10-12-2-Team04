package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.catalog.ImageExceptionCatalog;
import com.triptrace.domain.image.image.dto.ImageFileRequest;
import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.factory.ImageFactory;
import com.triptrace.domain.image.image.module.ImageInfo;
import com.triptrace.domain.image.image.module.ImageProcessor;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.image.image.support.ImageUrlResolver;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
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
    private final ImageProcessor imageProcessor;
    private final TripService tripService;
    private final ImageUrlResolver imageUrlResolver;

    //member service로 대체
    private final MemberRepository memberRepository;
    private Member getMember(Long ownerId){
        return memberRepository.findById(ownerId).orElseThrow(()->new ServiceException("404-1","사용자가 없습니다."));
    }
    private ImageUploadResponse upload(Long ownerId, Long tripId, MultipartFile imageFile) {
        String fileName = null;
        List<SavedFileInfo> savedFileInfoList;
        Image image;
        ImageServiceResponse imageServiceResponse = null;
        try {
            fileName = imageFile.getOriginalFilename();
            if( ! imageFile.getContentType().equals("image/jpeg")){
                throw ImageExceptionCatalog.invalid();
            }
            //extract
            ImageInfo imageInfo = imageProcessor.extract(imageFile.getInputStream());
            //image file save
            savedFileInfoList = imageProcessor.saveImageAll(imageFile.getInputStream(),imageInfo.getOrientation());
            if(savedFileInfoList.isEmpty()){
                throw ImageExceptionCatalog.invalid();
            }
            ImageFileRequest imageFileRequest = ImageFactory.createImageFileRequest(savedFileInfoList);
            //insert db
            Member owner = getMember(ownerId);
            Trip trip = tripService.findOwnedTrip(tripId, ownerId);
            image = ImageFactory.createImage(owner, trip, imageInfo, imageFileRequest);
            imageServiceResponse = imageService.create(image);
        }
        catch(IOException | ServiceException | ImageProcessException e){
            log.warn(ImageExceptionCatalog.invalid().toString());
            return ImageFactory.createImageUploadResponse(fileName, null);
        }
        return toPublicUrlResponse(ImageFactory.createImageUploadResponse(fileName, imageServiceResponse));
    }
    public List<ImageUploadResponse> uploadImages(Long ownerId, Long tripId, MultipartFile[] images){
        if(images == null || images.length == 0){
            throw ImageExceptionCatalog.invalid();
        }

        List<ImageUploadResponse> list = new ArrayList<>();

        ImageUploadResponse response;
        for (MultipartFile image : images) {
            response = upload(ownerId, tripId, image);
            list.add(response);
        }
        return list;
    }

    private ImageUploadResponse toPublicUrlResponse(ImageUploadResponse response) {
        if (response == null || response.uploadStatus() == null || response.uploadStatus().name().equals("FAILED")) {
            return response;
        }

        return new ImageUploadResponse(
            response.fileName(),
            response.id(),
            imageUrlResolver.toPublicUrl(response.originalFileUrl()),
            imageUrlResolver.toPublicUrl(response.thumbnailUrl()),
            response.mimeType(),
            response.uploadStatus()
        );
    }
}
