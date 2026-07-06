package com.triptrace.global.web;

import com.triptrace.domain.image.image.module.storage.ImageStorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final String uploadDir;
    private final String servingImagesPath;
    private final String thumbnailImagesPath;
    private final String profileImagesPath;

    public StaticResourceConfig(
        ImageStorageProperties imageStorageProperties
    ) {
        this.uploadDir = imageStorageProperties.upload().path();
        this.servingImagesPath = toResourceLocation(uploadDir+imageStorageProperties.upload().serving());
        this.thumbnailImagesPath = toResourceLocation(uploadDir+imageStorageProperties.upload().thumbnail());
        this.profileImagesPath = toResourceLocation(uploadDir+imageStorageProperties.upload().profile());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/serving/**")
            .addResourceLocations(servingImagesPath);
        registry.addResourceHandler("/images/thumbnail/**")
            .addResourceLocations(thumbnailImagesPath);
        registry.addResourceHandler("/images/profile/**")
            .addResourceLocations(profileImagesPath);
    }

    private String toResourceLocation(String path) {
        return Path.of(path).toAbsolutePath().normalize().toUri().toString();
    }
}
