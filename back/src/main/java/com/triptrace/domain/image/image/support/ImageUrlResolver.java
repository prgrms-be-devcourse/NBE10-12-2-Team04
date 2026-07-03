package com.triptrace.domain.image.image.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ImageUrlResolver {
    private final String servingImagesPath;
    private final String thumbnailImagesPath;

    public ImageUrlResolver(
        @Value("${custom.servingImage}") String servingImagesPath,
        @Value("${custom.thumbnailImage}") String thumbnailImagesPath
    ) {
        this.servingImagesPath = normalize(servingImagesPath);
        this.thumbnailImagesPath = normalize(thumbnailImagesPath);
    }

    public String toPublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://") || storedPath.startsWith("/images/")) {
            return storedPath;
        }

        String normalizedPath = normalize(storedPath);
        String filename = Path.of(normalizedPath).getFileName().toString();

        if (normalizedPath.startsWith(servingImagesPath)) {
            return "/images/serving/" + filename;
        }
        if (normalizedPath.startsWith(thumbnailImagesPath)) {
            return "/images/thumbnail/" + filename;
        }

        return storedPath;
    }

    private String normalize(String path) {
        return Path.of(path).normalize().toString().replace("\\", "/");
    }
}
