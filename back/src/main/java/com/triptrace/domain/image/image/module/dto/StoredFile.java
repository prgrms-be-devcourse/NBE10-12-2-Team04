package com.triptrace.domain.image.image.module.dto;

public record StoredFile(
    String path,
    String name,
    Long size
) {
}
