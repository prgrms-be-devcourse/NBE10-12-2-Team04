package com.triptrace.domain.image.image.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
        @NotBlank String servingUrl,
        @NotBlank String thumbnailUrl,
        @NotBlank String profileUrl) {}
    public record Thumbnail(
        @Min(1) int width,
        @Min(1) int height) {}
    public record Ext(
        @NotBlank String jpg) {}
}
