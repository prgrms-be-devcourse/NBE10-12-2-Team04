package com.triptrace.domain.image.image.module.storage;

import com.triptrace.domain.image.image.module.ExifOrientation;
import com.triptrace.domain.image.image.module.SavedFileInfo;
import com.triptrace.domain.image.image.module.dto.StoredFile;
import com.triptrace.domain.image.image.module.exception.ImageProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class ImageFileStorage {
    //TODO:ERROR CODE ENUM으로 변경
    //기본 처리 or 매개변수에서 못 읽음
    private static final String IMAGE_PROCESSING_ERROR = "400-2";
    //변확 혹은 이동 과정에서 읽어올 수 없음
    private static final String IMAGE_PROCESSING_READ_ERROR = "400-3";
    //저장하는 과정 중에서 저장 불가
    private static final String IMAGE_PROCESSING_SAVE_ERROR = "400-4";

    private static final String IMAGE_PROCESSING_DELETE_ERROR = "400-5";
    //upload 경로
    private final String uploadDir;
    //프로필 이미지 보관 경로
    private final String profileImagesPath;
    //서빙용 이미지 보관 경로
    private final String servingImagesPath;
    //섬네일 이미지 보관 경로
    private final String thumbnailImagesPath;
    //섬네일 이미지 크기
    private final int thumbnailWidth;
    private final int thumbnailHeight;
    //처리할 확장자
    private final String jpegExt;
    //파일 처리용
    private final FileStorage fileStorage;

    public ImageFileStorage(ImageStorageProperties properties, FileStorage fileStorage) {
        this.uploadDir = properties.upload().path();
        this.servingImagesPath = properties.upload().serving();
        this.thumbnailImagesPath = properties.upload().thumbnail();
        this.thumbnailWidth = properties.thumbnail().width();
        this.thumbnailHeight = properties.thumbnail().height();
        this.jpegExt = properties.ext().jpg();
        this.profileImagesPath = properties.upload().profile();
        this.fileStorage = fileStorage;
    }

    //profile이미지 진입점
    public String saveProfileImage(byte[] image) throws ImageProcessException {
        BufferedImage bufferedImage = getBufferedImage(image);
        StoredFile stored = saveImage(bufferedImage, uploadDir + "/" + profileImagesPath, generateFileName(jpegExt), false);
        return profileImagesPath + "/" + stored.name();
    }

    private StoredFile saveImage(BufferedImage image, String directoryPath, String fileName, boolean isThumbnail) throws ImageProcessException {
        long fileSize=0L;
        StoredFile storedFile = null;
        try {
            if(isThumbnail){
                image = resize(image, thumbnailWidth, thumbnailHeight);
            }
            image = convertToRGB(image);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            boolean written = ImageIO.write(image, jpegExt, bytes);
            if(!written){
                throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR, "파일을 저장할 수 없습니다.");
            }
            byte[] imageBytes = bytes.toByteArray();
            storedFile = fileStorage.save(imageBytes, directoryPath, fileName);
        }
        catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR,"파일을 저장할 수 없습니다.", e);
        }
        if(storedFile == null){
            throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR,"파일을 저장할 수 없습니다.");
        }
        return storedFile;
    }
    //image 처리용
    public SavedFileInfo saveImageWithThumbnail(byte[] image, ExifOrientation orientation) throws ImageProcessException {
        BufferedImage bufferedImage = getBufferedImage(image);
        bufferedImage = rotate(bufferedImage, orientation);
        StoredFile origin = saveImage(bufferedImage, uploadDir + servingImagesPath, generateFileName(jpegExt), false);
        StoredFile thumbnail = saveImage(bufferedImage, uploadDir + thumbnailImagesPath, generateFileName(jpegExt), true);
        return new SavedFileInfo(
            servingImagesPath + "/" + origin.name(),
            thumbnailImagesPath + "/"+ thumbnail.name(),
            origin.size(),
            "image/"+jpegExt);
    }
    //
    public boolean deleteImage(String imagePath) throws ImageProcessException {
        try {
            fileStorage.delete(uploadDir + imagePath);
        }catch (IOException e){
            log.warn(imagePath, e);
            throw new ImageProcessException(IMAGE_PROCESSING_DELETE_ERROR, "파일 삭제에 실패했습니다.");
        }
        return true;
    }


    //편의성
    private BufferedImage getBufferedImage(byte[] image) throws ImageProcessException {
        if (image == null) {
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR, "이미지를 읽을 수 없습니다.");
        }
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_READ_ERROR, "이미지를 읽을 수 없습니다.", e);
        }
        if (bufferedImage == null) {
            throw new ImageProcessException(IMAGE_PROCESSING_READ_ERROR, "이미지를 읽을 수 없습니다.");
        }
        return bufferedImage;
    }
    private BufferedImage convertToRGB(BufferedImage image) throws ImageProcessException{
        //ImageIO가 TYPE_INT_ARGB로 읽어서, 변환이 필요한 경우에 사용
        //JPEG는 ARGB를 지원하지 않음
        if(image==null){
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR, "이미지를 읽을 수 없습니다.");
        }
        if( ! image.getColorModel().hasAlpha()){return image;}
        BufferedImage rgbImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgbImage;
    }

    private BufferedImage rotate(BufferedImage image, ExifOrientation exifOrientation) {
        if(exifOrientation == null || exifOrientation == ExifOrientation.NORMAL){
            return image;
        }
        AffineTransform transform = new AffineTransform();
        switch (exifOrientation) {//1이 normal
            case ExifOrientation.ROTATE_180://180
                transform.translate(image.getWidth(), image.getHeight());
                transform.rotate(Math.PI);
                break;
            case ExifOrientation.ROTATE_90_CW://90
                transform.translate(image.getHeight(), 0);
                transform.rotate(Math.PI / 2);
                break;
            case ExifOrientation.ROTATE_270_CW://270
                transform.translate(0, image.getWidth());
                transform.rotate(-1 * Math.PI / 2);
                break;
        }
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        image = op.filter(image, null);
        return image;
    }

    private BufferedImage resize(BufferedImage image, int newWidth, int newHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = Math.min((double)newWidth/(double)width,(double)newHeight/(double)height);
        ratio = Math.min(ratio, 1.0); //더 작은 이미지가 나오면 변환하지 않도록
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
