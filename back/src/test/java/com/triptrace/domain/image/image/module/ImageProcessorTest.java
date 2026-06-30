package com.triptrace.domain.image.image.module;


import com.drew.imaging.ImageMetadataReader;
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


@SpringBootTest
public class ImageProcessorTest {

    //임시 저장소
    @TempDir
    Path tempDir;
    @Autowired
    ImageProcessor imageProcessor;

    @BeforeEach
    void setUp() {
        imageProcessor = new ImageProcessor(
            tempDir.resolve("serving").toString(),
            tempDir.resolve("thumbnail").toString()
        );
    }


    @Test
    @DisplayName("메타데이터 필요한 것만 추출")
    void test01() throws Exception {
        InputStream fis = getClass().getResourceAsStream("/kakaotalk-am.jpeg");
        ImageInfo info = imageProcessor.extract(fis);
        System.out.println(info);
        //초기값 덮어씌워졌는지 검증
        initImageInfoData(info);
    }

    @Test
    @DisplayName("반드시 성공됨 메타데이터 모두 출력")
    void test02() throws Exception {
        InputStream fis = getClass().getResourceAsStream("/kakaotalk-am.jpeg");
        imageProcessor.showAllInfoByMetaData(ImageMetadataReader.readMetadata(fis));
    }

    @Test
    @DisplayName("데이터추출 후 이미지 2개로 저장")
    void test03() throws Exception {
        byte[] bytes = getClass().getResourceAsStream("/kakaotalk-am.jpeg").readAllBytes();
        ByteArrayInputStream fis = new ByteArrayInputStream(bytes);
        ImageInfo imgInfo = imageProcessor.extract(fis);
        initImageInfoData(imgInfo);
        fis.reset();
        List<SavedFileInfo> list = imageProcessor.saveImageAll(fis, imgInfo.getOrientation());
        for(SavedFileInfo savedFileInfo : list) {
            System.out.println(savedFileInfo);
            assertThat(savedFileInfo).isNotNull();
            assertThat(savedFileInfo.path()).isNotNull();
            assertThat(savedFileInfo.size()).isGreaterThan(1);
        }
        assertThat(list.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("이미지 저장 시 파일 크기를 반환한다")
    void test05() throws Exception {
        byte[] bytes = getClass().getResourceAsStream("/kakaotalk-am.jpeg").readAllBytes();
        ImageInfo info = imageProcessor.extract(new ByteArrayInputStream(bytes));
        List<SavedFileInfo> list = imageProcessor.saveImageAll(new ByteArrayInputStream(bytes), info.getOrientation());
        for(SavedFileInfo savedFileInfo : list) {
            System.out.println(savedFileInfo);
            assertThat(savedFileInfo).isNotNull();
            assertThat(savedFileInfo.path()).isNotNull();
            assertThat(savedFileInfo.size()).isGreaterThan(1);
        }
        assertThat(list.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("유효하지 않은 파일 형식은 예외를 던진다")
    void test04() {
        InputStream is = new ByteArrayInputStream("not an image".getBytes());
        assertThatThrownBy(() -> imageProcessor.extract(is))
            .isInstanceOf(ImageProcessException.class);
    }

    private void initImageInfoData(ImageInfo info){
        assertThat(info).isNotNull();
        assertThat(info.getOrientation()).isGreaterThan(0);
        assertThat(info.getHeight()).isGreaterThan(0);
        assertThat(info.getWidth()).isGreaterThan(0);
        assertThat(info.getFileSize()).isGreaterThan(0);
        assertThat(info.getModel()).isNotBlank();
        assertThat(info.getMaker()).isNotBlank();
        assertThat(info.getMimeType()).isNotBlank();
        assertThat(info.getTimeZone()).isNotBlank();
        assertThat(info.getLatitude()).isNotEqualTo(360.0);
        assertThat(info.getLongitude()).isNotEqualTo(360.0);
        assertThat(info.getCapturedAt()).isNotEqualTo(new Date());
    }
}
