package com.triptrace.domain.image.image.module;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.triptrace.domain.image.image.module.dto.ImageDateTime;
import com.triptrace.domain.image.image.module.dto.ImageExifIF;
import com.triptrace.domain.image.image.module.dto.ImageLocation;
import com.triptrace.domain.image.image.module.dto.ImageWidthHeight;
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ImageMetadataExtractor {
    //TODO: ERROR CODE ENUM 타입으로 변경
    //ERROR CODE, 영역으로 구분
    private static final String FILE_EXTRACT_ERROR = "400-1";

    private void showAllInfoByMetaData(Metadata metadata){
        if(metadata == null) {
            log.warn("metadata is null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                sb.append(tag.toString()).append("\n");
            }
        }
        log.debug(sb.toString());
    }

    public ImageInfo extract(byte[] bytes) throws ImageProcessException {
        ImageInfo imageInfo = new ImageInfo();
        try (ByteArrayInputStream fis = new ByteArrayInputStream(bytes)){
            FileType fileType = FileTypeDetector.detectFileType(fis);
            imageInfo.setFileSize(Long.valueOf(bytes.length));
            if(!fileTypeFilter(fileType)) {
                throw new ImageProcessException(FILE_EXTRACT_ERROR,"파일 유형이 올바르지 않습니다.");
            }
            Metadata metadata = ImageMetadataReader.readMetadata(fis);
            showAllInfoByMetaData(metadata);
            log.debug("file type: {}", fileType);

            ImageDateTime imageDateTime = getImageDateTime(metadata);
            if(imageDateTime != null) {
                imageInfo.setCapturedAt(imageDateTime.dateTime());
                imageInfo.setTimeZone(imageDateTime.timeZone());
            }

            ImageExifIF imageExifIF = getExifIF(metadata);
            if(imageExifIF != null) {
                imageInfo.setMaker(imageExifIF.maker());
                imageInfo.setModel(imageExifIF.device());
                imageInfo.setOrientation(imageExifIF.orientation());
            }

            ImageWidthHeight imageWidthHeight = getWidthHeight(metadata);
            if(imageWidthHeight != null) {
                imageInfo.setWidth(imageWidthHeight.width());
                imageInfo.setHeight(imageWidthHeight.height());
            }

            ImageLocation imageLocation = getLocation(metadata);
            if(imageLocation != null) {
                imageInfo.setLatitude(imageLocation.latitude());
                imageInfo.setLongitude(imageLocation.longitude());
            }
        }
        catch(ImageProcessingException | ImageProcessException | IOException e){
            log.warn(e.getMessage());
            throw new ImageProcessException(FILE_EXTRACT_ERROR,"메타데이터를 추출할 수 없습니다.", e);
        }
        return imageInfo;
    }
    private ImageDateTime getImageDateTime(Metadata metadata) {
        ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        ImageDateTime imageDateTime = null;
        if (subIFDDirectory != null) {
            String dataTimeString = subIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            String timeZone = subIFDDirectory.getDescription(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL);
            LocalDateTime dateTime = null;
            try {
                if (dataTimeString != null) {
                    dateTime = LocalDateTime.parse(dataTimeString, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
                }
                log.debug("date: {}, timeZone: {}", dateTime, timeZone);
                imageDateTime = new ImageDateTime(dateTime, timeZone);
            }
            catch (Exception e) {
                log.warn("촬영일시 형식을 파싱할 수 없습니다.: {}", e.getMessage());
                return null;
            }
        } else log.warn("SubIFD directory is null");
        return imageDateTime;
    }
    private ImageExifIF getExifIF(Metadata metadata) {
        ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ImageExifIF imageExifIF = null;
        ExifOrientation exifOrientation = ExifOrientation.NORMAL;
        try {
            if (exifDirectory != null) {
                int orientation = exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                String make = exifDirectory.getDescription(ExifIFD0Directory.TAG_MAKE);
                String model = exifDirectory.getDescription(ExifIFD0Directory.TAG_MODEL);
                log.debug("orientation: {}", orientation);
                log.debug("make: {}", make);
                log.debug("model: {}", model);
                exifOrientation = ExifOrientation.fromExifValue(orientation);
                imageExifIF = new ImageExifIF(exifOrientation, model, make);
            } else log.warn("Exif directory is null");
        }catch(MetadataException e){
            log.warn("회전값, 기기 정보를 불러올 수 없습니다.: {}", e.getMessage());
            return null;
        }
        return imageExifIF;
    }
    private ImageWidthHeight getWidthHeight(Metadata metadata) {
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        ImageWidthHeight imageWidthHeight = null;
        try {
            if (jpegDirectory != null) {
                int height = jpegDirectory.getImageHeight();
                int width = jpegDirectory.getImageWidth();
                log.debug("height: {}, width: {}", height, width);
                imageWidthHeight = new ImageWidthHeight(width, height);
            } else log.warn("Jpeg directory is null");
        }catch(MetadataException e) {
            log.warn("이미지의 크기 정보를 불러올 수 없습니다.: {}", e.getMessage());
            return null;
        }
        return imageWidthHeight;
    }
    private ImageLocation getLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        ImageLocation imageLocation = null;
        if (gpsDirectory != null) {
            double latitude = gpsDirectory.getGeoLocation().getLatitude();
            double longitude = gpsDirectory.getGeoLocation().getLongitude();
            log.debug("Latitude is: {}", latitude);
            log.debug("Longitude is: {}", longitude);
            imageLocation = new ImageLocation(latitude, longitude);
        } else log.warn("GPS directory is null");
        return imageLocation;
    }
    private boolean fileTypeFilter(FileType fileType){
        log.debug("fileType: {}", fileType);
        return fileType == FileType.Jpeg;
    }
}


