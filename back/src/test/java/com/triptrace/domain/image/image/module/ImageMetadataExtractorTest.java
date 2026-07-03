package com.triptrace.domain.image.image.module;

import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ImageMetadataExtractorTest {

    ImageMetadataExtractor imageMetadataExtractor;

    String imageFileName = "/test-a-mail.jpg";
    byte[] imageBytes;

    @BeforeEach
    void setUp() throws IOException {
        imageMetadataExtractor = new ImageMetadataExtractor();
        try (InputStream is = getClass().getResourceAsStream(imageFileName)) {
            imageBytes = is.readAllBytes();
        }
    }

    @Test
    @DisplayName("정상 이미지에서 메타데이터를 모두 추출한다")
    void test01() {
        ImageInfo info = imageMetadataExtractor.extract(imageBytes);

        assertThat(info).isNotNull();
        assertThat(info.getOrientation()).isNotNull();
        assertThat(info.getHeight()).isGreaterThan(0);
        assertThat(info.getWidth()).isGreaterThan(0);
        assertThat(info.getFileSize()).isGreaterThan(0);
        assertThat(info.getModel()).isNotNull();
        assertThat(info.getMaker()).isNotNull();
        assertThat(info.getMimeType()).isNotBlank();
        assertThat(info.getTimeZone()).isNotNull();
        assertThat(info.getLatitude()).isNotNull();
        assertThat(info.getLongitude()).isNotNull();
        assertThat(info.getCapturedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 크기를 정확히 추출한다")
    void test02() {
        ImageInfo info = imageMetadataExtractor.extract(imageBytes);

        assertThat(info.getFileSize()).isEqualTo((long) imageBytes.length);
    }

    @Test
    @DisplayName("mimeType은 image/jpeg 형식으로 채워진다")
    void test03() {
        ImageInfo info = imageMetadataExtractor.extract(imageBytes);

        assertThat(info.getMimeType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("이미지 형식이 아니면 예외를 던진다")
    void test04() {
        byte[] invalid = "not an image".getBytes();

        assertThatThrownBy(() -> imageMetadataExtractor.extract(invalid))
            .isInstanceOf(ImageProcessException.class);
    }
}
