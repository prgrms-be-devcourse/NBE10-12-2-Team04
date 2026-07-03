package com.triptrace.domain.image.image.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//수정 못해야함
public record SavedFileInfo (
    @NotNull String servingUrl,
    @NotNull String thumbnailUrl,
    Long size,
    @NotBlank String mimeType){
}
