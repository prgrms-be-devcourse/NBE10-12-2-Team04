package com.triptrace.domain.member.member.service;

import com.triptrace.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Component
public class ProfileImageStorage {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final Path profileImagesPath;

    public ProfileImageStorage(@Value("${custom.profileImage}") String profileImagesPath) {
        this.profileImagesPath = Path.of(profileImagesPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("400-1", "업로드할 프로필 이미지가 없습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ServiceException("400-2", "프로필 이미지는 jpg, png, webp 파일만 업로드할 수 있습니다.");
        }

        try {
            Files.createDirectories(profileImagesPath);
            String extension = getExtension(file.getOriginalFilename(), file.getContentType());
            String storedFileName = "%s.%s".formatted(UUID.randomUUID(), extension);
            Path target = profileImagesPath.resolve(storedFileName);
            file.transferTo(target);

            return "/images/profile/" + storedFileName;
        } catch (IOException e) {
            throw new ServiceException("500-1", "프로필 이미지 저장에 실패했습니다.");
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (StringUtils.hasText(extension)) {
            return extension.toLowerCase();
        }
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }
}
