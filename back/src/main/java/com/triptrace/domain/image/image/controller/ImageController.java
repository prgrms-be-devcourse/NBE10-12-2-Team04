package com.triptrace.domain.image.image.controller;

import com.triptrace.domain.image.image.facade.ImageProcessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ImageController {
    private final ImageProcessFacade imageProcessFacade;

    @PostMapping("/{tripId}/images")
    public ResponseEntity<?> upload(
        @RequestParam Long ownerId,
        @PathVariable Long tripId,
        @RequestParam MultipartFile[] images
    ){
        imageProcessFacade.uploadImages(ownerId, images);
        return new ResponseEntity<>(
            HttpStatus.OK
        );
    }

}
