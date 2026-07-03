package com.triptrace.domain.image.image.module.dto;

import com.triptrace.domain.image.image.module.ExifOrientation;

public record ImageExifIF(ExifOrientation orientation, String device, String maker) {
}
