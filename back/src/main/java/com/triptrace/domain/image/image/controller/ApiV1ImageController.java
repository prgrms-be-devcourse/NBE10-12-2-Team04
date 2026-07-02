package com.triptrace.domain.image.image.controller;

import com.triptrace.domain.image.image.dto.ImageUploadResponse;
import com.triptrace.domain.image.image.facade.ImageDeleteFacade;
import com.triptrace.domain.image.image.facade.ImageModifyFacade;
import com.triptrace.domain.image.image.facade.ImageUploadFacade;
import com.triptrace.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ApiV1ImageController {
    private final ImageUploadFacade imageProcessFacade;
    private final ImageDeleteFacade imageDeleteFacade;
    private final ImageModifyFacade imageModifyFacade;

    @PostMapping("/trips/{tripId}/images")
    public RsData<List<ImageUploadResponse>> upload(
        @RequestParam Long ownerId,
        @PathVariable Long tripId,
        @RequestParam MultipartFile[] images
    ){
        return new RsData<>(
            "200-1",
            "업로드 되었습니다.",
            imageProcessFacade.uploadImages(ownerId,tripId, images)
        );
    }
    @DeleteMapping("/trips/{tripId}/posts/{postId}/images/{imageId}")
    public RsData<?> delete(
        @RequestParam Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long postId,
        @PathVariable Long imageId
    ){
        imageDeleteFacade.deleteById(ownerId,tripId,postId, imageId);
        return new RsData<>(
            "200-1",
            "삭제 되었습니다.",
            null
        );
    }

    @DeleteMapping("/trips/{tripId}/posts/{postId}/images")
    public RsData<?> delete(
        @RequestParam Long ownerId,
        @PathVariable Long tripId,
        @PathVariable Long postId,
        @RequestParam String imageUrl
    ){
        imageDeleteFacade.deleteByUrl(ownerId,tripId, postId, imageUrl);
        return new RsData<>(
            "200-1",
            "삭제 되었습니다.",
            null
        );
    }
    @PatchMapping("/trips/{tripId}/images")
    public RsData<?> modify(
        @RequestParam Long ownerId,
        @PathVariable Long tripId,
        @RequestParam Long postId,
        @RequestParam Long imageId
    ){

        return new RsData<>(
            "200-1",
            "수정 되었습니다.",
            imageModifyFacade.modifyById(ownerId,tripId,postId,imageId)
        );
    }

}
