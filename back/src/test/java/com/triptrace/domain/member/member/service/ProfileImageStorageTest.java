package com.triptrace.domain.member.member.service;

import com.triptrace.domain.image.image.processing.ImageProcessor;
import com.triptrace.domain.image.image.storage.ImageStorageProperties;
import com.triptrace.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageStorageTest {
    @TempDir
    Path tempDir;

    private ProfileImageStorage profileImageStorage;
    private byte[] imageBytes;

    @BeforeEach
    void setUp() throws IOException {
        ImageStorageProperties properties = new ImageStorageProperties(
            new ImageStorageProperties.Upload(
                tempDir.toString(),
                "serving",
                "thumbnail",
                "profile",
                "/serving",
                "/thumbnail",
                "/profile"
            ),
            new ImageStorageProperties.Thumbnail(1024, 1024),
            new ImageStorageProperties.Ext("jpeg")
        );
        profileImageStorage = new ProfileImageStorage(properties, new ImageProcessor());

        try (InputStream is = getClass().getResourceAsStream("/test-a-mail.jpg")) {
            imageBytes = is.readAllBytes();
        }
    }

    @Test
    @DisplayName("프로필 이미지를 저장하면 공개 URL을 반환하고 파일을 생성한다")
    void storeProfileImage() {
        String url = profileImageStorage.store(new MockMultipartFile(
            "image",
            "profile.jpg",
            "image/jpeg",
            imageBytes
        ));

        assertThat(url).startsWith("/profile/");
        assertThat(Files.exists(tempDir.resolve(url.substring(1)))).isTrue();
    }

    @Test
    @DisplayName("content type이 이미지여도 실제 이미지 바이트가 아니면 거부한다")
    void rejectInvalidImageBytes() {
        MockMultipartFile file = new MockMultipartFile(
            "image",
            "profile.jpg",
            "image/jpeg",
            "not an image".getBytes()
        );

        assertThatThrownBy(() -> profileImageStorage.store(file))
            .isInstanceOf(ServiceException.class);
    }
}
