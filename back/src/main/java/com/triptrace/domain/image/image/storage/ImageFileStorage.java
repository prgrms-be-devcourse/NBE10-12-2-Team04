package com.triptrace.domain.image.image.storage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.triptrace.domain.image.image.processing.ExifOrientation;
import com.triptrace.domain.image.image.processing.ImageProcessor;
import com.triptrace.domain.image.image.processing.dto.SavedFileInfo;
import com.triptrace.domain.image.image.processing.dto.StoredFile;
import com.triptrace.domain.image.image.processing.exception.ImageProcessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImageFileStorage {
    private static final String IMAGE_PROCESSING_SAVE_ERROR = "400-4";
    private static final String IMAGE_PROCESSING_DELETE_ERROR = "400-5";

    private final String uploadDir;
    private final String profileImagesPath;
    private final String servingImagesPath;
    private final String thumbnailImagesPath;
    private final String publicPrefix;
    private final int thumbnailWidth;
    private final int thumbnailHeight;
    private final String jpegExt;
    private final FileStorage fileStorage;
    private final ImageProcessor imageProcessor;

    public ImageFileStorage(ImageStorageProperties properties, FileStorage fileStorage) {
        this(properties, fileStorage, new ImageProcessor());
    }

    @Autowired
    public ImageFileStorage(ImageStorageProperties properties, FileStorage fileStorage, ImageProcessor imageProcessor) {
        this.uploadDir = properties.upload().path();
        this.servingImagesPath = properties.upload().servingPath();
        this.thumbnailImagesPath = properties.upload().thumbnailPath();
        this.thumbnailWidth = properties.thumbnail().width();
        this.thumbnailHeight = properties.thumbnail().height();
        this.jpegExt = properties.ext().jpg();
        this.profileImagesPath = properties.upload().profilePath();
        this.publicPrefix = properties.upload().publicPrefix();
        this.fileStorage = fileStorage;
        this.imageProcessor = imageProcessor;
    }

    public String saveProfileImage(byte[] image) throws ImageProcessException {
        BufferedImage bufferedImage = imageProcessor.read(image);
        StoredFile stored = saveImage(bufferedImage, resolveUploadPath(profileImagesPath), generateFileName(jpegExt),
            false);
        return profileImagesPath + "/" + stored.name();
    }

    private StoredFile saveImage(BufferedImage image, String directoryPath, String fileName, boolean isThumbnail)
        throws ImageProcessException {
        StoredFile storedFile = null;
        try {
            if (isThumbnail) {
                image = imageProcessor.resizeToFit(image, thumbnailWidth, thumbnailHeight);
            }
            byte[] imageBytes = imageProcessor.encodeJpeg(image, jpegExt);
            storedFile = fileStorage.save(imageBytes, directoryPath, fileName);
        } catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR, "파일을 저장할 수 없습니다.", e);
        }
        if (storedFile == null) {
            throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR, "파일을 저장할 수 없습니다.");
        }
        return storedFile;
    }

    public SavedFileInfo saveImageWithThumbnail(byte[] image, ExifOrientation orientation)
        throws ImageProcessException {
        BufferedImage bufferedImage = imageProcessor.read(image);
        bufferedImage = imageProcessor.rotate(bufferedImage, orientation);
        StoredFile origin = saveImage(bufferedImage, resolveUploadPath(servingImagesPath), generateFileName(jpegExt),
            false);
        StoredFile thumbnail;
        try {
            thumbnail = saveImage(bufferedImage, resolveUploadPath(thumbnailImagesPath), generateFileName(jpegExt),
                true);
        } catch (ImageProcessException e) {
            deleteImage(servingImagesPath + "/" + origin.name());
            throw e;
        }
        return new SavedFileInfo(
            servingImagesPath + "/" + origin.name(),
            thumbnailImagesPath + "/" + thumbnail.name(),
            origin.size(),
            "image/" + jpegExt);
    }

    public boolean deleteImage(String imagePath) throws ImageProcessException {
        try {
            fileStorage.delete(resolveStoragePath(imagePath));
        } catch (IOException e) {
            log.warn(imagePath, e);
            throw new ImageProcessException(IMAGE_PROCESSING_DELETE_ERROR, "파일 삭제에 실패했습니다.");
        }
        return true;
    }

    private String generateFileName(String fileExt) {
        return UUID.randomUUID() + "." + fileExt;
    }

    private String resolveUploadPath(String path) {
        return Path.of(uploadDir, path.replaceFirst("^/", "")).toString();
    }

    private String resolveStoragePath(String imagePath) {
        return resolveUploadPath(imagePath);
    }

    public void cleanUp(SavedFileInfo savedFileInfo) {
        String originFile = savedFileInfo.servingUrl();
        String thumbnailFile = savedFileInfo.thumbnailUrl();
        try {
            deleteImage(originFile);
        } catch (ImageProcessException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_DELETE_ERROR, "보상 트랜잭션 실패");
        }
        try {
            deleteImage(thumbnailFile);
        } catch (ImageProcessException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_DELETE_ERROR, "보상 트랜잭션 실패");
        }
    }
}
