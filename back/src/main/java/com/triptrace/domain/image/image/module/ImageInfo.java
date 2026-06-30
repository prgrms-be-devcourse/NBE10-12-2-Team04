package com.triptrace.domain.image.image.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter(value= AccessLevel.PACKAGE)
public class ImageInfo{
    private int  width;
    private int height;
    private double longitude;
    private double latitude;
    private Date capturedAt;
    private String timeZone;
    private String model;
    private String maker;
    private int orientation;// 1 정상, 3 180도, 6 90도 시계, 8 270도 시계
    private String mimeType;
    private long fileSize;
    ImageInfo(){
        width = height = 0;
        longitude = 360.0;
        latitude = 360.0;
        timeZone = "";
        model = "";
        maker = "";
        orientation = 0;
        mimeType = "";
        fileSize = 0;
        capturedAt = new Date();
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\n------------------splitter------------------");
        sb.append("\n width: " + width)
            .append("\n height: " + height)
            .append("\n longitude: " + longitude)
            .append("\n latitude: " + latitude)
            .append("\n capturedAt: " + capturedAt)
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
