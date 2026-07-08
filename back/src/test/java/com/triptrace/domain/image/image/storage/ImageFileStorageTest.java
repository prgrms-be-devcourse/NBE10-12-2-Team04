package com.triptrace.domain.image.image.storage;

import com.triptrace.domain.image.image.processing.ExifOrientation;
import com.triptrace.domain.image.image.processing.SavedFileInfo;
import com.triptrace.domain.image.image.processing.exception.ImageProcessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ImageFileStorageTest {

    @TempDir
    Path tempDir;

    ImageFileStorage imageFileStorage;

    String imageFileName = "/test-a-mail.jpg";
    byte[] imageBytes;

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
        imageFileStorage = new ImageFileStorage(properties, new LocalFileStorage());

        try (InputStream is = getClass().getResourceAsStream(imageFileName)) {
            imageBytes = is.readAllBytes();
        }
    }

    @Test
    @DisplayName("원본과 섬네일을 저장하면 각각의 URL과 파일 크기를 반환한다")
    void test01() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        assertThat(savedFileInfo).isNotNull();
        assertThat(savedFileInfo.servingUrl()).startsWith("/serving/");
        assertThat(savedFileInfo.thumbnailUrl()).startsWith("/thumbnail/");
        assertThat(savedFileInfo.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("저장된 원본과 섬네일 파일이 실제 디스크에 생성된다")
    void test02() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        assertThat(Files.exists(diskPath(savedFileInfo.servingUrl()))).isTrue();
        assertThat(Files.exists(diskPath(savedFileInfo.thumbnailUrl()))).isTrue();
    }

    @Test
    @DisplayName("섬네일은 지정한 크기 이하로 리사이즈된다")
    void test03() throws IOException {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);

        BufferedImage thumbnail = ImageIO.read(diskPath(savedFileInfo.thumbnailUrl()).toFile());

        assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(1024);
    }

    @Test
    @DisplayName("90도 회전을 적용하면 가로/세로가 뒤바뀐 크기로 저장된다")
    void test04() throws IOException {
        SavedFileInfo normal = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);
        SavedFileInfo rotated = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.ROTATE_90_CW);

        BufferedImage normalImage = ImageIO.read(diskPath(normal.servingUrl()).toFile());
        BufferedImage rotatedImage = ImageIO.read(diskPath(rotated.servingUrl()).toFile());

        assertThat(rotatedImage.getWidth()).isEqualTo(normalImage.getHeight());
        assertThat(rotatedImage.getHeight()).isEqualTo(normalImage.getWidth());
    }

    @Test
    @DisplayName("프로필 이미지를 저장하면 URL을 반환하고 실제 파일도 생성된다")
    void test05() {
        String url = imageFileStorage.saveProfileImage(imageBytes);

        assertThat(url).startsWith("/profile/");
        assertThat(Files.exists(diskPath(url))).isTrue();
    }

    @Test
    @DisplayName("이미지를 삭제하면 디스크에서 파일이 사라진다")
    void test06() {
        SavedFileInfo savedFileInfo = imageFileStorage.saveImageWithThumbnail(imageBytes, ExifOrientation.NORMAL);
        Path originFile = diskPath(savedFileInfo.servingUrl());
        assertThat(Files.exists(originFile)).isTrue();

        imageFileStorage.deleteImage(savedFileInfo.servingUrl());

        assertThat(Files.exists(originFile)).isFalse();
    }

    @Test
    @DisplayName("null 이미지를 저장하려 하면 예외를 던진다")
    void test07() {
        assertThatThrownBy(() -> imageFileStorage.saveImageWithThumbnail(null, ExifOrientation.NORMAL))
            .isInstanceOf(ImageProcessException.class);
    }

    @Test
    @DisplayName("이미지가 아닌 데이터를 저장하려 하면 예외를 던진다")
    void test08() {
        byte[] invalid = "not an image".getBytes();

        assertThatThrownBy(() -> imageFileStorage.saveImageWithThumbnail(invalid, ExifOrientation.NORMAL))
            .isInstanceOf(ImageProcessException.class);
    }

    private Path diskPath(String storedUrl) {
        return Paths.get(tempDir.toString() + storedUrl);
    }
}
