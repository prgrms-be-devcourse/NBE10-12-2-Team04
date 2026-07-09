package com.triptrace.domain.image.image.processing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExifOrientationTest {

    @Test
    @DisplayName("EXIF 값 1은 NORMAL로 매핑된다")
    void test01() {
        assertThat(ExifOrientation.fromExifValue(1)).isEqualTo(ExifOrientation.NORMAL);
    }

    @Test
    @DisplayName("EXIF 값 3은 ROTATE_180으로 매핑된다")
    void test02() {
        assertThat(ExifOrientation.fromExifValue(3)).isEqualTo(ExifOrientation.ROTATE_180);
    }

    @Test
    @DisplayName("EXIF 값 6은 ROTATE_90_CW로 매핑된다")
    void test03() {
        assertThat(ExifOrientation.fromExifValue(6)).isEqualTo(ExifOrientation.ROTATE_90_CW);
    }

    @Test
    @DisplayName("EXIF 값 8은 ROTATE_270_CW로 매핑된다")
    void test04() {
        assertThat(ExifOrientation.fromExifValue(8)).isEqualTo(ExifOrientation.ROTATE_270_CW);
    }

    @Test
    @DisplayName("정의되지 않은 값이 들어오면 NORMAL로 대체된다")
    void test05() {
        assertThat(ExifOrientation.fromExifValue(99)).isEqualTo(ExifOrientation.NORMAL);
    }
}
