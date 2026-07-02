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
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ImageProcessor {
    //TODO: 추후 front에서 사용할 크기 논의 후 변경
    private static final int THUMBNAIL_WIDTH = 1024;
    private static final int THUMBNAIL_HEIGHT = 1024;
    private static final String JPEG_EXT = "jpeg";

    //ERROR CODE, 영역으로 구분
    private static final String FILE_EXTRACT_ERROR = "400-1";
    private static final String IMAGE_PROCESSING_ERROR = "400-2";

    //서빙용 이미지 보관 경로
    private final String servingImagesPath;
    //섬네일 이미지 보관 경로
    private final String thumbnailImagesPath;

    public ImageProcessor(
        @Value("${custom.servingImage}")    String serving,
        @Value("${custom.thumbnailImage}")  String thumbnail) {
        this.servingImagesPath = serving;
        this.thumbnailImagesPath = thumbnail;
    }

    public void showAllInfoByMetaData(Metadata metadata){
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

    public ImageInfo extract(InputStream imageStream) throws ImageProcessException {
        //TODO: directory별로 쪼갤지 고민
        ImageInfo imageInfo = new ImageInfo();
        try {
            byte[] bytes = imageStream.readAllBytes();
            ByteArrayInputStream fis = new ByteArrayInputStream(bytes);
            FileType fileType = FileTypeDetector.detectFileType(fis);
            log.debug("fileType: {}", fileType);
            imageInfo.setFileSize(bytes.length);
            if(!fileTypeFilter(fileType)) {
                throw new ImageProcessException(FILE_EXTRACT_ERROR,"파일 유형이 올바르지 않습니다.");
            }

            Metadata metadata = ImageMetadataReader.readMetadata(fis);

            //directory init
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

            log.debug("file type: {}", fileType);
            imageInfo.setMimeType("image/" + fileType.toString().toLowerCase());

            if (gpsDirectory != null) {
                double Latitude = gpsDirectory.getGeoLocation().getLatitude();
                double Longitude = gpsDirectory.getGeoLocation().getLongitude();
                log.debug("Latitude is: {}", Latitude);
                log.debug("Longitude is: {}", Longitude);
                imageInfo.setLatitude(Latitude);
                imageInfo.setLongitude(Longitude);
            } else log.warn("GPS directory is null");

            if (jpegDirectory != null) {
                int height = jpegDirectory.getImageHeight();
                int width = jpegDirectory.getImageWidth();
                log.debug("height: {}, width: {}", height, width);
                imageInfo.setHeight(height);
                imageInfo.setWidth(width);
            } else log.warn("Jpeg directory is null");

            if (exifDirectory != null) {
                int orientation = exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                String make = exifDirectory.getDescription(ExifIFD0Directory.TAG_MAKE);
                String model = exifDirectory.getDescription(ExifIFD0Directory.TAG_MODEL);
                log.debug("orientation: {}", orientation);
                log.debug("make: {}", make);
                log.debug("model: {}", model);
                imageInfo.setOrientation(orientation);
                imageInfo.setMaker(make);
                imageInfo.setModel(model);
            } else log.warn("Exif directory is null");

            if (subIFDDirectory != null) {
                    String dataTimeString = subIFDDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                    String timeZone = subIFDDirectory.getDescription(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL);
                    LocalDateTime dateTime = null;
                try {
                    if (dataTimeString != null) {
                        dateTime = LocalDateTime.parse(dataTimeString, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
                    }
                    log.debug("date: {}, timeZone: {}", dateTime, timeZone);
                    imageInfo.setCapturedAt(dateTime);
                    imageInfo.setTimeZone(timeZone);
                }
                catch (Exception e) {
                    log.warn("촬영일시 형식을 파싱할 수 없습니다.: {}", dataTimeString);
                    imageInfo.setCapturedAt(null);
                    imageInfo.setTimeZone(null);
                }
            } else log.warn("SubIFD directory is null");
        }
        catch(MetadataException | ImageProcessingException | ImageProcessException | IOException e){
            throw new ImageProcessException(FILE_EXTRACT_ERROR,"메타데이터를 추출할 수 없습니다.", e);
        }
        return imageInfo;
    }

    private boolean fileTypeFilter(FileType fileType){
        //당장은 jpeg만 나중에 png 같은 거 포함되면 여기에 추가
        return fileType == FileType.Jpeg;
    }

    public List<SavedFileInfo> saveImageAll(InputStream inputStream, int orientation) throws ImageProcessException, IOException {
        List<SavedFileInfo> savedFileInfoList = new ArrayList<SavedFileInfo>();
        BufferedImage image = ImageIO.read(inputStream);
        if(orientation != 1) {
            AffineTransform transform = new AffineTransform();
            switch (orientation) {//1이 normal
                case 3://180
                    transform.translate(image.getWidth(), image.getHeight());
                    transform.rotate(Math.PI);
                    break;
                case 6://90
                    transform.translate(image.getHeight(), 0);
                    transform.rotate(Math.PI / 2);
                    break;
                case 8://270
                    transform.translate(0, image.getWidth());
                    transform.rotate(-1 * Math.PI / 2);
                    break;
            }
            AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            image = op.filter(image, null);
        }
        savedFileInfoList.add(saveImage(image, servingImagesPath));
        savedFileInfoList.add(saveImage(image, thumbnailImagesPath));
        return savedFileInfoList;
    }

    private BufferedImage convertToRGB(BufferedImage image) {
        //ImageIO가 TYPE_INT_ARGB로 읽어서, 변환이 필요한 경우에 사용
        //JPEG는 ARGB를 지원하지 않음
        BufferedImage rgbImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgbImage;
    }

    private SavedFileInfo saveImage(BufferedImage image, String directoryPath) throws ImageProcessException {
        long fileSize=0L;
        SavedFileInfo savedFileInfo;
        try {
            if (image == null) {
                throw new ImageProcessException(IMAGE_PROCESSING_ERROR,"이미지를 읽을 수 없습니다.");
            }
            if(directoryPath.compareTo(thumbnailImagesPath) == 0){
                image = resize(image, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            }
            Path path = Paths.get(directoryPath);
            Files.createDirectories(path);
            String encodeFileName = generateFileName(JPEG_EXT);
            Path outputPath = path.resolve(encodeFileName);
            File outputFile = outputPath.toFile();
            image = convertToRGB(image);
            ImageIO.write(image, JPEG_EXT, outputFile);
            fileSize = outputFile.length();
            savedFileInfo = new SavedFileInfo(outputFile.getPath(), fileSize);
        }
        catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR,"파일을 저장할 수 없습니다.", e);
        }
        if(savedFileInfo == null){
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR,"파일을 저장할 수 없습니다.");
        }
        return savedFileInfo;
    }

    private BufferedImage resize(BufferedImage image, int newWidth, int newHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = Math.min((double)newWidth/(double)width,(double)newHeight/(double)height);
        int scaleHeight = (int)(height*ratio);
        int scaleWidth =  (int)(width*ratio);

        BufferedImage resized = new BufferedImage(scaleWidth, scaleHeight, image.getType());

        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, scaleWidth, scaleHeight, null);
        g2d.dispose();
        return resized;
    }

    private String generateFileName(String fileExt){
        String newFileName = UUID.randomUUID().toString() + "." + fileExt;
        return newFileName;
    }
}
