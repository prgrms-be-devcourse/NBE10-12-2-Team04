package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.module.ImageInfo;
import com.triptrace.domain.image.image.module.ImageProcessor;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessFacade {
    private final ImageService imageService;
    private final ImageProcessor imageProcessor;

    public void upload(Long ownerId, Long tripId, MultipartFile image) {
        String fileName;
        List<SavedFileInfo> savedFileInfoList;
        try {
            fileName = image.getOriginalFilename();
            if( ! image.getContentType().equals("image/jpeg")){
                throw new ServiceException("400-1", "잘못된 이미지 형식입니다.");
            }
            //extract
            ImageInfo imageInfo = imageProcessor.extract(image.getInputStream());
            //save
            savedFileInfoList = imageProcessor.saveImageAll(image.getInputStream(),imageInfo.getOrientation());
            //insert db

        }
        catch(IOException e){
            throw new ServiceException("401-1", "이미지를 처리할 수 없습니다.");
        }
    }
    public void uploadImages(Long ownerId, Long tripId, MultipartFile[] images){
        if(images==null || images.length==0){
            throw new ServiceException("400-1","처리할 이미지가 없습니다.");
        }
        for (MultipartFile image : images) {
            upload(ownerId, tripId, image);
        }
    }

}
