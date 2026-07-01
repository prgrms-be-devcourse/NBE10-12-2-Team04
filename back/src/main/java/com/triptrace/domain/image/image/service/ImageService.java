package com.triptrace.domain.image.image.service;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;

    @Transactional
    public ImageServiceResponse create(
        Member owner,
        Trip trip,
        String originalFileUrl,
        String thumbnailFileUrl,
        Long fileSize,
        String mimeType,
        BigDecimal gpsLat,
        BigDecimal gpsLng,
        LocalDateTime capturedAt,
        String deviceInfo
    ){
        return new ImageServiceResponse(imageRepository.save(new Image(
            owner,
            trip,
            null,
            null,
            originalFileUrl,
            thumbnailFileUrl,
            fileSize,
            mimeType,
            gpsLat,
            gpsLng,
            capturedAt,
            deviceInfo
        )));
    }
}
