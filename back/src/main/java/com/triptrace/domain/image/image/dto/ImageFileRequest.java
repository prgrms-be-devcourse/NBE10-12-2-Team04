package com.triptrace.domain.image.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageFileRequest {
    private String ImageFileUrl;
    private String thumbnailImageFileUrl;
    private Long fileSize;
}
