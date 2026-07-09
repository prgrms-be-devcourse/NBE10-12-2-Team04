package com.triptrace.domain.image.image.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "custom")
@Validated
public record ImageStorageProperties(
    @NotNull @Valid Upload upload,
    @NotNull @Valid Thumbnail thumbnail,
    @NotNull @Valid Ext ext
) {
    public record Upload(
        @NotBlank String path,
        @NotBlank String servingPath,
        @NotBlank String thumbnailPath,
        @NotBlank String profilePath,
        @NotBlank String publicPrefix) {
    }

    public record Thumbnail(
        @Min(1) int width,
        @Min(1) int height) {
    }

    public record Ext(
        @NotBlank String jpg) {
    }
}
