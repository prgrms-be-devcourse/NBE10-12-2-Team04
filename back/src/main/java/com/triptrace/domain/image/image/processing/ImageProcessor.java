package com.triptrace.domain.image.image.processing;

import com.triptrace.domain.image.image.processing.exception.ImageProcessException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageProcessor {
    private static final String IMAGE_PROCESSING_ERROR = "400-2";
    private static final String IMAGE_PROCESSING_READ_ERROR = "400-3";
    private static final String IMAGE_PROCESSING_SAVE_ERROR = "400-4";

    public BufferedImage read(byte[] image) {
        if (image == null) {
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR, "이미지를 읽을 수 없습니다.");
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            if (bufferedImage == null) {
                throw new ImageProcessException(IMAGE_PROCESSING_READ_ERROR, "이미지를 읽을 수 없습니다.");
            }
            return bufferedImage;
        } catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_READ_ERROR, "이미지를 읽을 수 없습니다.", e);
        }
    }

    public BufferedImage rotate(BufferedImage image, ExifOrientation exifOrientation) {
        if (exifOrientation == null || exifOrientation == ExifOrientation.NORMAL) {
            return image;
        }
        AffineTransform transform = new AffineTransform();
        switch (exifOrientation) {
            case ROTATE_180 -> {
                transform.translate(image.getWidth(), image.getHeight());
                transform.rotate(Math.PI);
            }
            case ROTATE_90_CW -> {
                transform.translate(image.getHeight(), 0);
                transform.rotate(Math.PI / 2);
            }
            case ROTATE_270_CW -> {
                transform.translate(0, image.getWidth());
                transform.rotate(-1 * Math.PI / 2);
            }
            case NORMAL -> {
            }
        }
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(image, null);
    }

    public BufferedImage resizeToFit(BufferedImage image, int newWidth, int newHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = Math.min((double) newWidth / (double) width, (double) newHeight / (double) height);
        ratio = Math.min(ratio, 1.0);
        int scaleHeight = (int) (height * ratio);
        int scaleWidth = (int) (width * ratio);

        BufferedImage resized = new BufferedImage(scaleWidth, scaleHeight, image.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, scaleWidth, scaleHeight, null);
        g2d.dispose();
        return resized;
    }

    public byte[] encodeJpeg(BufferedImage image, String jpegExt) {
        BufferedImage rgbImage = convertToRGB(image);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            boolean written = ImageIO.write(rgbImage, jpegExt, bytes);
            if (!written) {
                throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR, "파일을 저장할 수 없습니다.");
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new ImageProcessException(IMAGE_PROCESSING_SAVE_ERROR, "파일을 저장할 수 없습니다.", e);
        }
    }

    private BufferedImage convertToRGB(BufferedImage image) {
        if (image == null) {
            throw new ImageProcessException(IMAGE_PROCESSING_ERROR, "이미지를 읽을 수 없습니다.");
        }
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage rgbImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgbImage;
    }
}
