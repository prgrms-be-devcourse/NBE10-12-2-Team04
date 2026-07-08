package com.triptrace.domain.image.image.processing;

public enum ExifOrientation {
    NORMAL( 1, 0 ),
    ROTATE_180( 3, 180 ),
    ROTATE_90_CW( 6, 90 ),
    ROTATE_270_CW( 8, 270 );
    private final int exifValue;
    private final int rotationDegrees;

    ExifOrientation(int exifValue, int rotationDegrees) {
        this.exifValue       = exifValue;
        this.rotationDegrees = rotationDegrees;
    }

    public static ExifOrientation fromExifValue(int exifValue) {
        for (ExifOrientation o : values()) {
            if (o.exifValue == exifValue) return o;
        }
        return NORMAL;
    }
}
