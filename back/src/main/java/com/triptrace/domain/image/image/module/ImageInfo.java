package com.triptrace.domain.image.image.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter(value= AccessLevel.PACKAGE)
public class ImageInfo{
    private int  width;
    private int height;
    private Double longitude;
    private Double latitude;
    private LocalDateTime capturedAt;
    private String timeZone;
    private String model;
    private String maker;
    private int orientation;// 1 정상, 3 180도, 6 90도 시계, 8 270도 시계
    private String mimeType;
    private long fileSize;
    ImageInfo(){
        width = height = 0;
        longitude = null;
        latitude = null;
        timeZone = null;
        model = null;
        maker = null;
        orientation = 0;
        mimeType = null;
        fileSize = 0;
        capturedAt = null;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------splitter------------------");
        sb.append("\n width: " + width)
            .append("\n height: " + height)
            .append("\n longitude: " + longitude)
            .append("\n latitude: " + latitude)
            .append("\n capturedAt: " + capturedAt.toString())
            .append("\n timeZone: " + timeZone)
            .append("\n model: " + model)
            .append("\n maker: " + maker)
            .append("\n orientation: " + orientation)
            .append("\n mimeType: " + mimeType)
            .append("\n fileSize: " + fileSize);
        sb.append("\n------------------splitter------------------");
        return sb.toString();
    }
}
