package com.triptrace.domain.image.image.processing.dto;

public record StoredFile(
    String path,
    String name,
    Long size
) {
}
