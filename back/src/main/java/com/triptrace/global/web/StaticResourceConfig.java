package com.triptrace.global.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final String servingImagesPath;
    private final String thumbnailImagesPath;
    private final String profileImagesPath;

    public StaticResourceConfig(
        @Value("${custom.servingImage}") String servingImagesPath,
        @Value("${custom.thumbnailImage}") String thumbnailImagesPath,
        @Value("${custom.profileImage}") String profileImagesPath
    ) {
        this.servingImagesPath = toResourceLocation(servingImagesPath);
        this.thumbnailImagesPath = toResourceLocation(thumbnailImagesPath);
        this.profileImagesPath = toResourceLocation(profileImagesPath);
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
