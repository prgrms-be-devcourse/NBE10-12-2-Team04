package com.triptrace.domain.image.image.processing.dto;

import com.triptrace.domain.image.image.processing.ExifOrientation;

public record ImageExifIF(ExifOrientation orientation, String device, String maker) {
}
