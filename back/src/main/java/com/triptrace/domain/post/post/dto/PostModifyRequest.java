package com.triptrace.domain.post.post.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostModifyRequest(
    LocalDate date,
    @Size(max = 100)
    String title,
    String memo
) {
}
